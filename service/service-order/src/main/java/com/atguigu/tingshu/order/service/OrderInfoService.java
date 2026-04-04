package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo>
{


    OrderInfoVo trade(TradeVo tradeVo,
                      Long userId);

    String submitOrder(OrderInfoVo orderInfoVo, Long userId);

    OrderInfo saveOrder(OrderInfoVo orderInfoVo, Long userId, String orderNo);

    void orderPaySuccess(String orderNo);

    void orderCancel(Long orderId);

    OrderInfo getOrderInfoByOrderNo(String orderNo);

    IPage<OrderInfo> findUserPage(Page<OrderInfo> pageParam, Long userId);
}
