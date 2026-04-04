package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService
{

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    @Override
    public void addUserAccount(Long userId)
    {
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccountMapper.insert(userAccount);
    }

    @Override
    public BigDecimal getAvailableAmount(Long userId)
    {
        UserAccount userAccount = this.getUserAccountByUserId(userId);
        return userAccount.getAvailableAmount();
    }

    @Override
    public UserAccount getUserAccountByUserId(Long userId)
    {
        return this.getOne(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId,
                                                                    userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int checkAndDeduct(AccountLockVo accountDeductVo)
    {
        int count = userAccountMapper.checkAndDeduct(accountDeductVo.getUserId(), accountDeductVo.getAmount());
        if (count == 0)
        {
            throw new GuiguException(400, "账户余额不足！");
        }
        this.addUserAccountDetail(
                accountDeductVo.getUserId(),
                accountDeductVo.getContent(),
                SystemConstant.ACCOUNT_TRADE_TYPE_MINUS,
                accountDeductVo.getAmount(),
                accountDeductVo.getOrderNo()
        );
        return count;
    }

    private void addUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo)
    {
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(title);
        userAccountDetail.setTradeType(tradeType);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetailMapper.insert(userAccountDetail);
    }
}
