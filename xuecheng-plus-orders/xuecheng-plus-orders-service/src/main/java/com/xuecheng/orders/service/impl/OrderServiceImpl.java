package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author CCL
 * @version 1.0
 * @description 订单相关的接口实现
 * @createTime 2024-09-17 17:56
 **/
@Service
public class OrderServiceImpl implements OrderService {

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
