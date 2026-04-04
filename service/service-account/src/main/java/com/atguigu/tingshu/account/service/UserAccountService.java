package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount>
{


    void addUserAccount(Long userId);

    BigDecimal getAvailableAmount(Long userId);


    UserAccount getUserAccountByUserId(Long userId);

    int checkAndDeduct(AccountLockVo accountDeductVo);
}
