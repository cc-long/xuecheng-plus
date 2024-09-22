package com.xuecheng.orders.service;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcPayRecord;

/**
 * @author CCL
 * @version 1.0
 * @description 订单相关的service接口
 * @createTime 2024-09-17 17:56
 **/
public interface OrderService {

    /**
     * 创建商品订单
     * @param userId 用户id
     * @param addOrderDto 添加的订单信息
     * @return 支付记录（包括二维码）
     */
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);

    /**
     * 查询支付记录 根据支付记录号
     * @param payNo 支付记录号
     * @return
     */
    public XcPayRecord getPayRecordByPayNo(String payNo);

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付记录号
     * @return 支付记录信息
     */
    public PayRecordDto queryPayResult(String payNo);

    /**
     * 保存支付状态
     * @param payStatusDto
     */
    public void saveAliPayStatus(PayStatusDto payStatusDto);

    /**
     * 发送通知结果
     * @param message
     */
    public void notifyPayResult(MqMessage message);
}
