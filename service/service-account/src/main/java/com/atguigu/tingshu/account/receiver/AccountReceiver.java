package com.atguigu.tingshu.account.receiver;


import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class AccountReceiver
{

    private final UserAccountService userAccountService;

    public AccountReceiver(UserAccountService userAccountService)
    {
        this.userAccountService = userAccountService;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(value = MqConst.EXCHANGE_USER , durable = "true"),
                    value = @Queue(value = MqConst.QUEUE_USER_REGISTER , durable = "true"),
                    key = {MqConst.ROUTING_USER_REGISTER}
            )
    )
    public void receive(Long userId , Message message , Channel channel) throws IOException
    {
        if(userId != null)
        {
            log.info("注册新的用户");
            userAccountService.addUserAccount(userId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
