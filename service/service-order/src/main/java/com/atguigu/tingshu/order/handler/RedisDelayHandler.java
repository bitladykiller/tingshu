package com.atguigu.tingshu.order.handler;


import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
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
        Thread consumerThread = new Thread(() ->
        {
            RBlockingQueue<Object> blockingQueue = redissonClient.getBlockingQueue(MqConst.EXCHANGE_CANCEL_ORDER);
            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
                    Object orderId = blockingQueue.take();
                    if (orderId != null)
                    {
                        orderInfoService.orderCancel(Long.parseLong(orderId.toString()));
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    log.warn("订单取消延迟队列监听线程已中断");
                }
                catch (Exception e)
                {
                    log.error("处理订单取消延迟消息失败", e);
                }
            }
        }, "order-cancel-delay-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }
}
