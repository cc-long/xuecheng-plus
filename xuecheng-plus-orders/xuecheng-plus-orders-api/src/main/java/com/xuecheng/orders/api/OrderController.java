package com.xuecheng.orders.api;

import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.service.OrderService;
import com.xuecheng.orders.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

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
}
