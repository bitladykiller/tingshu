package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.VipServiceConfigFeignClient;
import com.atguigu.tingshu.user.client.impl.UserInfoDegradeFeignClient;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;
    @Autowired
    private UserAccountFeignClient userAccountFeignClient;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private OrderDerateMapper orderDerateMapper;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private RedissonClient redissonClient;

    private void sendDelayMessage(Long orderId)
    {
        RBlockingQueue<Object> blockingQueue = redissonClient.getBlockingQueue(MqConst.EXCHANGE_CANCEL_ORDER);
        RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        delayedQueue.offer(orderId.toString(), 30, TimeUnit.MINUTES);
    }


    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId)
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
            Assert.notNull(albumInfoResult, "返回专辑结果集不能为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑对象不能为空");

            if (userInfoVo.getIsVip()
                          .intValue() == 0)
            {
                originalAmount = albumInfo.getPrice();
                if (albumInfo.getDiscount()
                             .intValue() != -1)
                {
                    derateAmount = originalAmount
                            .multiply(new BigDecimal("10").subtract(albumInfo.getDiscount()))
                            .divide(new BigDecimal(10), 2, RoundingMode.HALF_UP);
                }
                orderAmount = originalAmount.subtract(derateAmount);
            }
            else
            {
                originalAmount = albumInfo.getPrice();
                if (albumInfo.getVipDiscount()
                             .intValue() != -1)
                {
                    derateAmount = albumInfo.getPrice()
                                            .multiply(new BigDecimal(10).subtract(albumInfo.getVipDiscount()))
                                            .divide(new BigDecimal(10), 2, RoundingMode.HALF_UP);
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

        }
        else if (tradeVo.getItemType()
                        .equals(SystemConstant.ORDER_ITEM_TYPE_TRACK))
        {
            if (tradeVo.getTrackCount()
                       .intValue() < 0)
            {
                throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
            }

            Result<List<TrackInfo>> trackInfoListResult =
                    trackInfoFeignClient.findPaidTrackInfoList(tradeVo.getItemId(), tradeVo.getTrackCount());
            Assert.notNull(trackInfoListResult, "返回声音列表结果集不能为空");
            List<TrackInfo> trackInfoList = trackInfoListResult.getData();
            Assert.notNull(trackInfoList, "声音列表不能为空");

            Result<AlbumInfo> albumInfoResult =
                    albumInfoFeignClient.getAlbumInfo(trackInfoList.get(0)
                                                                   .getAlbumId());
            Assert.notNull(albumInfoResult, "返回专辑结果集不能为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑对象不能为空");

            originalAmount = tradeVo.getTrackCount()
                                    .intValue() > 0
                    ? albumInfo.getPrice()
                               .multiply(new BigDecimal(tradeVo.getTrackCount()))
                    : albumInfo.getPrice();

            orderAmount = originalAmount;

            orderDetailVoList = trackInfoList.stream()
                                             .map(trackInfo ->
                                                  {
                                                      OrderDetailVo orderDetailVo = new OrderDetailVo();
                                                      orderDetailVo.setItemId(trackInfo.getId());
                                                      orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                                                      orderDetailVo.setItemPrice(albumInfo.getPrice());
                                                      orderDetailVo.setItemName(trackInfo.getTrackTitle());
                                                      return orderDetailVo;
                                                  })
                                             .collect(Collectors.toList());

        }
        else if (tradeVo.getItemType()
                        .equals(SystemConstant.ORDER_ITEM_TYPE_VIP))
        {
            Result<VipServiceConfig> vipServiceConfigResult =
                    vipServiceConfigFeignClient.getVipServiceConfig(tradeVo.getItemId());
            Assert.notNull(vipServiceConfigResult, "返回vip配置结果集不能为空");
            VipServiceConfig vipServiceConfig = vipServiceConfigResult.getData();
            Assert.notNull(vipServiceConfig, "返回vip配置对象不能为空");

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
                             .replace("-", "");
        redisTemplate.opsForValue()
                     .set(tradeNoKey, tradeNo);

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

        Map<String, Object> parameterMap =
                JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
        String sign = SignHelper.getSign(parameterMap);
        orderInfoVo.setSign(sign);

        return orderInfoVo;
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public String submitOrder(OrderInfoVo orderInfoVo, Long userId)
    {
        Map map = JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
        map.put("payWay", SystemConstant.ORDER_PAY_WAY_WEIXIN);
        SignHelper.checkSign(map);

        String tradeNo = orderInfoVo.getTradeNo();
        if (StringUtils.isEmpty(tradeNo))
        {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        String tradeNoKey = "user:trade:" + userId;
        String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        Boolean flag = (Boolean) redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class),
                                                       Arrays.asList(tradeNoKey), tradeNo);
        if (!flag)
        {
            throw new GuiguException(ResultCodeEnum.ORDER_SUBMIT_REPEAT);
        }
        String orderNo = UUID.randomUUID()
                             .toString()
                             .replace("-", "");
        if (!SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfoVo.getPayWay()))
        {
            this.saveOrder(orderInfoVo, userId, orderNo);
        }
        else
        {
            try
            {
                AccountLockVo accountDeductVo = new AccountLockVo();
                accountDeductVo.setOrderNo(orderNo);
                accountDeductVo.setUserId(userId);
                accountDeductVo.setAmount(orderInfoVo.getOrderAmount());
                accountDeductVo.setContent(orderInfoVo.getOrderDetailVoList()
                                                      .get(0)
                                                      .getItemName());
                Result result = userAccountFeignClient.checkAndDeduct(accountDeductVo);
                if (200 != result.getCode())
                {
                    throw new GuiguException(result.getCode(), result.getMessage());
                }
                OrderInfo orderInfo = this.saveOrder(orderInfoVo, userId, orderNo);
                UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
                userPaidRecordVo.setOrderNo(orderNo);
                userPaidRecordVo.setUserId(orderInfo.getUserId());
                userPaidRecordVo.setItemType(orderInfo.getItemType());
                List<Long> itemIdList = orderInfoVo.getOrderDetailVoList()
                                                   .stream()
                                                   .map(OrderDetailVo::getItemId)
                                                   .collect(Collectors.toList());
                userPaidRecordVo.setItemIdList(itemIdList);
                Result userResult = userInfoFeignClient.savePaidRecord(userPaidRecordVo);
                if (200 != userResult.getCode())
                {
                    throw new GuiguException(211, "新增购买记录异常");
                }
                return orderNo;

            }
            catch (GuiguException e)
            {
                e.printStackTrace();
                throw new GuiguException(e.getCode(), e.getMessage());
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }
        }
        return orderNo;
    }

    @Transactional
    @Override
    public OrderInfo saveOrder(OrderInfoVo orderInfoVo, Long userId, String orderNo)
    {
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoVo, orderInfo);
        orderInfo.setOrderNo(orderNo);
        String orderTitle = orderInfoVo.getOrderDetailVoList()
                                       .get(0)
                                       .getItemName();
        orderInfo.setOrderTitle(orderTitle);
        orderInfo.setUserId(userId);
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        orderInfoMapper.insert(orderInfo);

        if (!CollectionUtils.isEmpty(orderInfoVo.getOrderDetailVoList()))
        {
            orderInfoVo.getOrderDetailVoList()
                       .forEach(orderDetailVo ->
                                {
                                    OrderDetail orderDetail = new OrderDetail();
                                    BeanUtils.copyProperties(orderDetailVo, orderDetail);
                                    orderDetail.setOrderId(orderInfo.getId());
                                    orderDetailMapper.insert(orderDetail);
                                });
        }

        if (!CollectionUtils.isEmpty(orderInfoVo.getOrderDerateVoList()))
        {
            orderInfoVo.getOrderDerateVoList()
                       .forEach(orderDerateVo ->
                                {
                                    OrderDerate orderDerate = new OrderDerate();
                                    BeanUtils.copyProperties(orderDerateVo, orderDerate);
                                    orderDerate.setOrderId(orderInfo.getId());
                                    orderDerateMapper.insert(orderDerate);
                                });
        }

        if (SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfo.getPayWay()))
        {
            this.orderPaySuccess(orderNo);
        }
        else
        {
            rabbitService.sendDealyMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER,
                                           orderInfo.getId(), MqConst.CANCEL_ORDER_DELAY_TIME);
        }
        return orderInfo;
    }

    @Override
    public void orderPaySuccess(String orderNo)
    {
        OrderInfo orderInfo = this.getOrderInfoByOrderNo(orderNo);
        if (orderInfo.getOrderStatus()
                     .equals(SystemConstant.ORDER_STATUS_PAID))
        {
            return;
        }
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
        this.updateById(orderInfo);
        if (orderInfo.getPayWay()
                     .equals(SystemConstant.ORDER_PAY_WAY_WEIXIN))
        {
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            userPaidRecordVo.setOrderNo(orderNo);
            userPaidRecordVo.setUserId(orderInfo.getUserId());
            userPaidRecordVo.setItemType(orderInfo.getItemType());
            List<Long> itemIdList = orderInfo.getOrderDetailList()
                                             .stream()
                                             .map(OrderDetail::getItemId)
                                             .collect(Collectors.toList());
            userPaidRecordVo.setItemIdList(itemIdList);
            this.userInfoFeignClient.savePaidRecord(userPaidRecordVo);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orderCancel(Long orderId)
    {
        OrderInfo orderInfo = this.getById(orderId);
        if (orderInfo == null)
        {
            return;
        }

        if (!SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus()))
        {
            return;
        }

        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_CANCEL);
        this.updateById(orderInfo);
    }

    public OrderInfo getOrderInfoByOrderNo(String orderNo)
    {
        OrderInfo orderInfo = this.getOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderInfo.getId()));

        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.setPayWayName(this.getPayWayName(orderInfo.getPayWay()));
        return orderInfo;
    }

    @Override
    public IPage<OrderInfo> findUserPage(Page<OrderInfo> pageParam, Long userId)
    {
        IPage<OrderInfo> infoIPage = orderInfoMapper.selectUserPage(pageParam, userId);
        infoIPage.getRecords()
                 .forEach(item ->
                          {
                              item.setOrderStatusName(getOrderStatusName(item.getOrderStatus()));
                              item.setPayWayName(getPayWayName(item.getPayWay()));
                          });
        return infoIPage;
    }


    private String getOrderStatusName(String orderStatus)
    {
        String orderStatusName = "";
        if (SystemConstant.ORDER_STATUS_UNPAID.equals(orderStatus))
        {
            orderStatusName = "未支付";
        }
        else if (SystemConstant.ORDER_STATUS_PAID.equals(orderStatus))
        {
            orderStatusName = "已支付";
        }
        else
        {
            orderStatusName = "已取消";
        }
        return orderStatusName;
    }

    private String getPayWayName(String payWay)
    {
        return SystemConstant.ORDER_PAY_WAY_WEIXIN.equals(payWay) ? "微信支付" : "余额支付";
    }

}
