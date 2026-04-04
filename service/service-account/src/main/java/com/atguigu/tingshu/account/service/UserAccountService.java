package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount>
{


    void addUserAccount(Long userId);

    BigDecimal getAvailableAmount(Long userId);


    UserAccount getUserAccountByUserId(Long userId);

    int checkAndDeduct(AccountLockVo accountDeductVo);

    void add(Long userId, BigDecimal rechargeAmount, String orderNo, String accountTradeTypeDeposit, String 充值);

    IPage<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> pageParam, Long userId);

    IPage<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> pageParam, Long userId);
}
