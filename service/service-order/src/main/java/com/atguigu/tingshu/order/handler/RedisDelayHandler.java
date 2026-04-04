package com.atguigu.tingshu.order.handler;


import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RedisDelayHandler
{
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private OrderInfoService orderInfoService;

    @PostConstruct
    public void listener()
    {
        new Thread(() ->
                   {
                       while (true)
                       {
                           RBlockingQueue<Object> blockingQueue = redissonClient.getBlockingQueue(
                                   MqConst.EXCHANGE_CANCEL_ORDER);
                           try
                           {
                               String orderId = blockingQueue.take()
                                                             .toString();
                               if (StringUtils.hasText(orderId))
                               {
                                   orderInfoService.orderCancel(Long.parseLong(orderId));
                               }
                           }
                           catch (InterruptedException e)
                           {
                               throw new RuntimeException(e);
                           }
                       }
                   });
    }
}
