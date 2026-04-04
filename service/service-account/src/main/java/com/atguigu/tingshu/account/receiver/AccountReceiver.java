package com.atguigu.tingshu.account.receiver;


import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Slf4j
@Component
public class AccountReceiver
{
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private RechargeInfoService rechargeInfoService;


    public AccountReceiver(UserAccountService userAccountService)
    {
        this.userAccountService = userAccountService;
    }

    @SneakyThrows
    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(value = MqConst.EXCHANGE_USER, durable = "true"),
                    value = @Queue(value = MqConst.QUEUE_USER_REGISTER, durable = "true"),
                    key = {MqConst.ROUTING_USER_REGISTER}
            )
    )
    public void receive(Long userId, Message message, Channel channel) throws IOException
    {
        if (userId != null)
        {
            log.info("注册新的用户");
            userAccountService.addUserAccount(userId);
        }
        channel.basicAck(message.getMessageProperties()
                                .getDeliveryTag(), false);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_RECHARGE_PAY_SUCCESS, durable = "true"),
            key = {MqConst.ROUTING_RECHARGE_PAY_SUCCESS}
    ))
    public void rechargePaySuccess(String orderNo, Message message, Channel channel)
    {
        log.info("充值成功通知: {}", orderNo);
        if (StringUtils.hasText(orderNo))
        {
            rechargeInfoService.rechargePaySuccess(orderNo);
        }

        //手动应答
        channel.basicAck(message.getMessageProperties()
                                .getDeliveryTag(), false);
    }
}
