package com.atguigu.tingshu.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.client.RechargeInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderInfoFeignClient;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService
{
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;
    @Autowired
    private RechargeInfoFeignClient rechargeInfoFeignClient;
    @Autowired
    private RabbitService rabbitService;

    @Override
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo)
    {
        PaymentInfo paymentInfo = this.getOne(
                new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        if (null == paymentInfo)
        {
            paymentInfo = new PaymentInfo();
            if (paymentType.equals(SystemConstant.PAYMENT_TYPE_ORDER))
            {
                Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderNo);
                Assert.notNull(orderInfoResult, "返回订单对象不能为空");
                OrderInfo orderInfo = orderInfoResult.getData();
                Assert.notNull(orderInfo, "返回订单对象不能为空");
                paymentInfo.setUserId(orderInfo.getUserId());
                paymentInfo.setContent(orderInfo.getOrderTitle());
                paymentInfo.setAmount(orderInfo.getOrderAmount());
            }
            else
            {
                Result<RechargeInfo> rechargeInfoResult = rechargeInfoFeignClient.getRechargeInfo(orderNo);
                Assert.notNull(rechargeInfoResult, "返回充值对象不能不为空");
                RechargeInfo rechargeInfo = rechargeInfoResult.getData();
                Assert.notNull(rechargeInfo, "返回充值对象不能不为空");
                paymentInfo.setUserId(rechargeInfo.getUserId());
                paymentInfo.setContent("充值");
                paymentInfo.setAmount(rechargeInfo.getRechargeAmount());
            }
            paymentInfo.setPaymentType(paymentType);
            paymentInfo.setOrderNo(orderNo);
            paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);
            this.save(paymentInfo);
        }
        return paymentInfo;
    }

    @Override
    public void updatePaymentStatus(Transaction transaction)
    {
        PaymentInfo paymentInfo = this.getOne(
                new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, transaction.getOutTradeNo()));
        if (paymentInfo.getPaymentStatus() == SystemConstant.PAYMENT_STATUS_PAID)
        {
            return;
        }

        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_PAID);
        paymentInfo.setOrderNo(transaction.getOutTradeNo());
        paymentInfo.setOutTradeNo(transaction.getTransactionId());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSON.toJSONString(transaction));
        this.updateById(paymentInfo);

        String routing = paymentInfo.getPaymentType()
                                    .equals(SystemConstant.PAYMENT_TYPE_ORDER) ? MqConst.ROUTING_ORDER_PAY_SUCCESS : MqConst.ROUTING_RECHARGE_PAY_SUCCESS;
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, routing, paymentInfo.getOrderNo());
    }

    @Override
    public boolean closePayment(String orderNo)
    {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_CANCEL);
        boolean result = this.update(paymentInfo,
                                     new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        return result;
    }
}
