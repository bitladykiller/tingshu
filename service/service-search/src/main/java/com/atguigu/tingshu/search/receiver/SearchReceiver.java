package com.atguigu.tingshu.search.receiver;


import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.search.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class SearchReceiver
{
    private SearchService searchService;

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_ALBUM_UPPER, durable = "true"),
            key = {MqConst.ROUTING_ALBUM_UPPER}
    ))
    public void upper_listen(Long id, Message message, Channel channel)
    {
        if (id != null)
        {
            searchService.upperAlbum(id);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM),
            value = @Queue(value = MqConst.QUEUE_ALBUM_LOWER),
            key = {MqConst.ROUTING_ALBUM_LOWER}
    ))
    public void Lower_listen(Long id, Message message, Channel channel)
    {
        if (id != null)
            searchService.lowerAlbum(id);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
