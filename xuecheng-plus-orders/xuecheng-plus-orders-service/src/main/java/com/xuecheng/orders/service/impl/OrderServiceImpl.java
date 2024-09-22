package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author CCL
 * @version 1.0
 * @description 订单相关的接口实现
 * @createTime 2024-09-17 17:56
 **/
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;

    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Autowired
    private MqMessageService mqMessageService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderServiceImpl currentProxy;

    @Autowired
    private XcOrdersMapper ordersMapper;

    @Autowired
    private XcOrdersGoodsMapper ordersGoodsMapper;

    @Autowired
    private XcPayRecordMapper payRecordMapper;

    @Value("${pay.qrcodeurl}")
    private String qrCodeeUrl;

    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {

        //插入订单表，订单主表和订单明细表
        //进行幂等性的判断，同一个选课记录只能有一个订单
        XcOrders xcOrders = savaXcOrders(userId, addOrderDto);


        //插入支付记录表
        XcPayRecord payRecord = createPayRecord(xcOrders);

        //生成二维码
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        Long payNo = payRecord.getPayNo();
        //获取支付二维码的url
        String url = String.format(qrCodeeUrl, payNo);
        //二维码图片
        String qrCode = null;
        try {
            qrCode = qrCodeUtil.createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码出错");
        }

        //构造返回的对象
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(qrCode);

        return payRecordDto;
    }

    @Override
    public XcPayRecord getPayRecordByPayNo(String payNo) {

        return payRecordMapper.selectOne(
                new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo,payNo));
    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        //调用支付宝的接口 查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);

        //拿到支付结果更新支付记录表和订单表的支付状态
        currentProxy.saveAliPayStatus(payStatusDto);

        //返回最新的支付记录信息
        XcPayRecord payRecordByPayNo = getPayRecordByPayNo(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecordByPayNo,payRecordDto);
        return payRecordDto;
    }

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, "RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        //bizContent.put("trade_no", "2014112611001004680073956707");
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            //调用ali的api 进行查询
            response = alipayClient.execute(request);
            if(!response.isSuccess()){
                XueChengPlusException.cast("支付宝查询异常");
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            XueChengPlusException.cast("查询支付结果异常");
        }

        String resultJson = response.getBody();
        //转map
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map<String,String> alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");

        //构造支付结果信息
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setTrade_no(alipay_trade_query_response.get("trade_no"));//交易流水号
        payStatusDto.setTrade_status(alipay_trade_query_response.get("trade_status"));//交易状态
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTotal_amount(alipay_trade_query_response.get("total_amount"));//总金额
        payStatusDto.setOut_trade_no(alipay_trade_query_response.get("out_trade_no"));
        return payStatusDto;
    }

    /**
     * 保存支付宝支付结果
     * @param payStatusDto 支付结果信息 支付宝查询的信息
     */
    @Transactional
    public void saveAliPayStatus(PayStatusDto payStatusDto){
        //支付记录号
        String outTradeNo = payStatusDto.getOut_trade_no();

        XcPayRecord payRecordByPayNo = getPayRecordByPayNo(outTradeNo);
        if (payRecordByPayNo == null){
            XueChengPlusException.cast("未找到相关支付记录");
        }
        Long orderId = payRecordByPayNo.getOrderId();
        XcOrders xcOrders = ordersMapper.selectById(orderId);
        if (xcOrders == null){
            XueChengPlusException.cast("未找到关联订单");
        }

        //数据库支付状态
        String statusFromDb = payRecordByPayNo.getStatus();
        if ("601002".equals(statusFromDb)){
            //如果支付成功
            return;
        }

        //如果支付成功
        String tradeStatus = payStatusDto.getTrade_status();
        if ("TRADE_SUCCESS".equals(tradeStatus)){
            //更新支付记录表的 支付状态
            payRecordByPayNo.setStatus("601002");
            //支付宝的订单号
            payRecordByPayNo.setOutPayNo(payStatusDto.getTrade_no());
            //第三方支付渠道编号
            payRecordByPayNo.setOutPayChannel("Alipay");
            //支付成功时间
            payRecordByPayNo.setPaySuccessTime(LocalDateTime.now());
            payRecordMapper.updateById(payRecordByPayNo);

            //更新订单表的状态
            xcOrders.setStatus("600002");//订单状态 成功
            ordersMapper.updateById(xcOrders);

            //将消息写到数据库
            MqMessage mqMessage = mqMessageService.addMessage("payresult_notify", xcOrders.getOutBusinessId(), xcOrders.getOrderType(), null);
            //发送消息
            notifyPayResult(mqMessage);

        }
    }

    @Override
    public void notifyPayResult(MqMessage message) {

        //消息内容
        String jsonString = JSON.toJSONString(message);

        //创建一个持久化消息
        Message messageObj = MessageBuilder.withBody(jsonString.getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();


        //消息id
        Long id = message.getId();
        //全局的消息id
        CorrelationData correlationData = new CorrelationData(id.toString());

        //使用correlationData指定回调方法
        correlationData.getFuture().addCallback(result -> {
            if (result.isAck()){
                //消息发送到了交换机
                log.debug("发送消息成功:{}", jsonString);
                //将消息从数据库表（mq_message）删除
                mqMessageService.completed(id);
            }else {
                //消息发送失败
                log.debug("发送消息失败:{}", jsonString);

            }
        },ex -> {
            //发生异常了
            log.debug("发送消息异常:{}", jsonString);
        });

        //发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT, "", messageObj, correlationData);
    }

    /**
     * 保存订单信息
     * @param userId
     * @param addOrderDto
     * @return
     */
    public XcOrders savaXcOrders(String userId, AddOrderDto addOrderDto){

        //幂等性的判断，同一个选课记录只能有一个订单
        XcOrders xcOrders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (xcOrders != null){
            return xcOrders;
        }

        //插入订单表，订单主表和
        xcOrders = new XcOrders();
        //使用雪花算法生成订单号
        xcOrders.setId(IdWorkerUtils.getInstance().nextId());
        xcOrders.setTotalPrice(addOrderDto.getTotalPrice());
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("600001");//未支付
        xcOrders.setUserId(userId);
        xcOrders.setOrderType(addOrderDto.getOrderType());//订单类型
        xcOrders.setOrderName(addOrderDto.getOrderName());
        xcOrders.setOrderDescrip(addOrderDto.getOrderDescrip());
        xcOrders.setOrderDetail(addOrderDto.getOrderDetail());
        xcOrders.setOutBusinessId(addOrderDto.getOutBusinessId());//如果是选课，这里记录选课表的主键id
        int insert = ordersMapper.insert(xcOrders);
        if (insert <= 0){
            XueChengPlusException.cast("添加订单失败");
        }

        //订单id
        Long orderId = xcOrders.getId();
        //插入订单明细表
        //将前端传入的明细json串转成lList
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        //遍历xcOrdersGoods插入订单明细表
        xcOrdersGoods.forEach(goods -> {
            goods.setOrderId(orderId);
            //插入订单明细
            ordersGoodsMapper.insert(goods);
        });


        return xcOrders;
    }

    /**
     * 查询订单-根据业务id，业务id是选课记录表中的主键
     * @param businessId 业务id
     * @return
     */
    public XcOrders getOrderByBusinessId(String businessId){

        XcOrders xcOrders = ordersMapper.selectOne(
                new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId)
        );


        return xcOrders;
    }

    /**
     * 保存支付记录
     * @param orders 订单信息
     * @return
     */
    public XcPayRecord createPayRecord(XcOrders orders){
        //订单id
        Long ordersId = orders.getId();
        XcOrders xcOrders = ordersMapper.selectById(ordersId);
        //如果此订单不存在 就不在添加支付记录
        if (xcOrders == null){
            XueChengPlusException.cast("订单不存在");
        }
        //订单的状态
        String status = xcOrders.getStatus();
        if ("601002".equals(status)){
            XueChengPlusException.cast("此订单已支付");
        }
        XcPayRecord xcPayRecord = new XcPayRecord();
        xcPayRecord.setPayNo(IdWorkerUtils.getInstance().nextId());//支付记录号,将来传给支付宝
        xcPayRecord.setOrderId(ordersId);
        xcPayRecord.setOrderName(xcOrders.getOrderName());
        xcPayRecord.setTotalPrice(xcOrders.getTotalPrice());
        xcPayRecord.setCurrency("CNY");
        xcPayRecord.setCreateDate(LocalDateTime.now());
        xcPayRecord.setStatus("601001");//未支付
        xcPayRecord.setUserId(xcOrders.getUserId());

        int insert = payRecordMapper.insert(xcPayRecord);
        if (insert <= 0){
            XueChengPlusException.cast("插入支付记录失败");
        }

        return xcPayRecord;
    }
}
