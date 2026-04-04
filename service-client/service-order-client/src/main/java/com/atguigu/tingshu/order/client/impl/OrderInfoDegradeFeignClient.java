package com.atguigu.tingshu.order.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.OrderInfoFeignClient;
import org.springframework.stereotype.Component;

@Component
public class OrderInfoDegradeFeignClient implements OrderInfoFeignClient {

    @Override
    public Result<OrderInfo> getOrderInfo(String orderNo)
    {
        return Result.fail();
    }
}
