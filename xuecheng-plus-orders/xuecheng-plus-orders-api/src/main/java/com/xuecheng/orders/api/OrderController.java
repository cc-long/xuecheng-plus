package com.xuecheng.orders.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import com.xuecheng.orders.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author CCL
 * @version 1.0
 * @description 订单相关的接口
 * @createTime 2024-09-17 17:50
 **/
@Controller
@Api(value = "订单支付接口", tags = "订单支付接口")
@Slf4j
public class OrderController {

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;

    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Value("&{pay.backnotifyurl}")
    String BACK_NOTIFY_URL;

    @Autowired
    private OrderService orderService;

    @ApiOperation("生成支付二维码")
    @PostMapping("/generatepaycode")
    @ResponseBody
    public PayRecordDto generatePayCode(@RequestBody AddOrderDto addOrderDto){

        //调用service，完成插入订单信息、插入支付记录、生成支付二维码
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        PayRecordDto payRecordDto = orderService.createOrder(user.getId(), addOrderDto);
        return payRecordDto;
    }

    @ApiOperation("扫码下单接口")
    @GetMapping("/requestpay")
    public void requestPay(String payNo, HttpServletResponse httpResponse) throws AlipayApiException, IOException {

        //传入了支付记录号，查询支付记录是否存在
        XcPayRecord payRecord = orderService.getPayRecordByPayNo(payNo);
        if (payRecord == null){
            XueChengPlusException.cast("支付记录不存在");
        }
        //判断是否支付
        if ("601002".equals(payRecord.getStatus())){
            XueChengPlusException.cast("已支付，无需重复支付");
        }

        //获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE);
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//创建API对应的request
//        alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
        alipayRequest.setNotifyUrl(BACK_NOTIFY_URL + "/orders/paynotify");//在公共参数中设置回跳和通知地址

        //构造业务参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        bizContent.put("total_amount", payRecord.getTotalPrice());
        bizContent.put("subject", payRecord.getOrderName());
        bizContent.put("product_code", "QUICK_WAP_WAY");
        alipayRequest.setBizContent(bizContent.toString());

        //调用SDK生成表单
        String form = alipayClient.pageExecute(alipayRequest).getBody();
        httpResponse.setContentType("text/html;charset=" + AlipayConfig.CHARSET);

        //直接将完整的表单html输出到页面
        httpResponse.getWriter().write(form);
        httpResponse.getWriter().flush();
    }

    @ApiOperation("查询支付结果接口")
    @GetMapping("payresult")
    @ResponseBody
    public PayRecordDto payresult(String payNo) {

        //查询支付状态
        //当支付成功后跟新支付记录表的支付状态 以及订单表当中的状态 为支付成功
        return orderService.queryPayResult(payNo);
    }

    @PostMapping("/paynotify")
    public void paynotify(HttpServletRequest request, HttpServletResponse response) throws IOException, AlipayApiException {

        //获取支付宝POST过来反馈信息
        Map<String,String> params = new HashMap<String,String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]:valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
//            valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }

        //获取支付宝的通知返回参数，可参考技术文档中页面跳转同步通知参数列表(以上仅供参考)//
        //计算得出通知验证结果
        //boolean AlipaySignature.rsaCheckV1(Map<String, String> params, String publicKey, String charset, String sign_type)
        boolean verify_result = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, AlipayConfig.CHARSET, "RSA2");

        if(verify_result){//验证成功
            //////////////////////////////////////////////////////////////////////////////////////////
            //获取支付宝的通知返回参数，可参考技术文档中页面跳转同步通知参数列表(以下仅供参考)//
            //商户订单号

            String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"),"UTF-8");
            //支付宝交易号

            String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"),"UTF-8");

            //交易状态
            String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"),"UTF-8");

            //交易状态
            String total_amount = new String(request.getParameter("total_amount").getBytes("ISO-8859-1"),"UTF-8");


            //请在这里加上商户的业务逻辑程序代码


            //——请根据您的业务逻辑来编写程序（以下代码仅作参考）——

            if(trade_status.equals("TRADE_SUCCESS")){
                //跟新支付记录表的支付状态 订单表的状态
                PayStatusDto payStatusDto = new PayStatusDto();
                payStatusDto.setTrade_status(trade_status);
                payStatusDto.setApp_id(APP_ID);
                payStatusDto.setTrade_no(trade_no);
                payStatusDto.setOut_trade_no(out_trade_no);
                payStatusDto.setTotal_amount(total_amount);
                orderService.saveAliPayStatus(payStatusDto);
            }

            //——请根据您的业务逻辑来编写程序（以上代码仅作参考）——
            response.getWriter().write("success");

            //////////////////////////////////////////////////////////////////////////////////////////
        }else{//验证失败
            response.getWriter().write("fail");
        }
    }
}
