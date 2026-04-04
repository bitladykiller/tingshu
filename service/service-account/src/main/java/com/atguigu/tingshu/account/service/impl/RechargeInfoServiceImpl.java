package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService
{

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;

    @Autowired
    private UserAccountService userAccountService;

    @Override
    public RechargeInfo getRechargeInfoByOrderNo(String orderNo)
    {
        return this.getOne(new LambdaQueryWrapper<RechargeInfo>().eq(RechargeInfo::getOrderNo, orderNo));
    }

    @Override
    public String submitRecharge(RechargeInfoVo rechargeInfoVo, Long userId)
    {
        //	创建对象
        RechargeInfo rechargeInfo = new RechargeInfo();
        //	赋值操作
        rechargeInfo.setUserId(userId);
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
        rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());
        rechargeInfo.setOrderNo(UUID.randomUUID()
                                    .toString()
                                    .replaceAll("-", ""));
        //	插入数据库
        rechargeInfoMapper.insert(rechargeInfo);
        //	返回订单编号
        return rechargeInfo.getOrderNo();
    }

    @Override
    @Transactional
    public void rechargePaySuccess(String orderNo)
    {
        RechargeInfo rechargeInfo = this.getRechargeInfoByOrderNo(orderNo);
        if (SystemConstant.ORDER_STATUS_PAID.equals(rechargeInfo.getRechargeStatus())) return;
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
        this.updateById(rechargeInfo);
        userAccountService.add(rechargeInfo.getUserId(), rechargeInfo.getRechargeAmount(), rechargeInfo.getOrderNo(),
                               SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, "充值");
    }
}