package com.xuecheng.orders.service;

import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;

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
}
