package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.VipServiceConfigFeignClient;
import com.atguigu.tingshu.user.client.impl.UserInfoDegradeFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService
{

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private UserInfoDegradeFeignClient userInfoFeignClient;
    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private VipServiceConfigFeignClient vipServiceConfigFeignClient;


    @Override
    public OrderInfoVo trade(TradeVo tradeVo,
                             Long userId)
    {
        Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
        Assert.notNull(userInfoVoResult);
        UserInfoVo userInfoVo = userInfoVoResult.getData();
        Assert.notNull(userInfoVo);
        BigDecimal originalAmount = new BigDecimal("0.00");
        BigDecimal derateAmount = new BigDecimal("0.00");
        BigDecimal orderAmount = new BigDecimal("0.00");
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        if (tradeVo.getItemType()
                   .equals(SystemConstant.ORDER_ITEM_TYPE_ALBUM))
        {
            Result<Boolean> isPaidAlbumResult = userInfoFeignClient.isPaidAlbum(tradeVo.getItemId());
            Assert.notNull(isPaidAlbumResult);
            Boolean isPaidAlbum = isPaidAlbumResult.getData();
            Assert.notNull(isPaidAlbum);
            if (isPaidAlbum)
            {
                throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
            }
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(tradeVo.getItemId());
            Assert.notNull(albumInfoResult,
                           "返回专辑结果集不能为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo,
                           "专辑对象不能为空");
            if (userInfoVo.getIsVip()
                          .intValue() == 0)
            {
                originalAmount = albumInfo.getPrice();
                if (albumInfo.getDiscount()
                             .intValue() != -1)
                {
                    derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getDiscount()))
                                                 .divide(new BigDecimal(10),
                                                         2,
                                                         RoundingMode.HALF_UP);
                }
                orderAmount = originalAmount.subtract(derateAmount);
            } else
            {
                originalAmount = albumInfo.getPrice();
                if (albumInfo.getVipDiscount()
                             .intValue() != -1)
                {
                    derateAmount = albumInfo.getPrice()
                                            .multiply(new BigDecimal(10).subtract(albumInfo.getVipDiscount()))
                                            .divide(new BigDecimal(10),
                                                    2,
                                                    RoundingMode.HALF_UP);
                }
                orderAmount = originalAmount.subtract(derateAmount);
            }
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(albumInfo.getPrice());
            orderDetailVoList.add(orderDetailVo);

            if (originalAmount.subtract(orderAmount)
                              .doubleValue() != 0)
            {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setDerateAmount(originalAmount.subtract(orderAmount));
                orderDerateVoList.add(orderDerateVo);
            }
        } else if (tradeVo.getItemType()
                          .equals(SystemConstant.ORDER_ITEM_TYPE_VIP))
        {
            Result<VipServiceConfig> vipServiceConfigResult = vipServiceConfigFeignClient.getVipServiceConfig(tradeVo.getItemId());
            Assert.notNull(vipServiceConfigResult,
                           "返回vip配置结果集不能为空");
            VipServiceConfig vipServiceConfig = vipServiceConfigResult.getData();
            Assert.notNull(vipServiceConfig,
                           "返回vip配置对象不能为空");

            originalAmount = vipServiceConfig.getPrice();
            derateAmount = vipServiceConfig.getPrice()
                                           .subtract(vipServiceConfig.getDiscountPrice());
            orderAmount = originalAmount.subtract(derateAmount);

            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName("VIP会员" + vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVo.setItemPrice(vipServiceConfig.getDiscountPrice());
            orderDetailVoList.add(orderDetailVo);

            if (originalAmount.subtract(orderAmount)
                              .doubleValue() != 0)
            {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                orderDerateVo.setDerateAmount(originalAmount.subtract(orderAmount));
                orderDerateVoList.add(orderDerateVo);
            }
        }
        String tradeNoKey = "user:trade:" + userId;
        String tradeNo = UUID.randomUUID()
                             .toString()
                             .replace("-",
                                      "");
        redisTemplate.opsForValue()
                     .set(tradeNoKey,
                          tradeNo);

        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setItemType(tradeVo.getItemType());
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setTradeNo(tradeNo);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        orderInfoVo.setTimestamp(SignHelper.getTimestamp());
        orderInfoVo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        Map<String, Object> parameterMap = JSON.parseObject(JSON.toJSONString(orderInfoVo),
                                                            Map.class);
        String sign = SignHelper.getSign(parameterMap);
        orderInfoVo.setSign(sign);
        return orderInfoVo;
    }
}
