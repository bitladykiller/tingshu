package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account/userAccount")
@SuppressWarnings({"all"})
public class UserAccountApiController
{

    @Autowired
    private UserAccountService userAccountService;

    @GuiGuLogin
    @Operation(summary = "获取账号可用金额")
    @GetMapping("getAvailableAmount")
    public Result<BigDecimal> getAvailableAmount()
    {
        return Result.ok(userAccountService.getAvailableAmount(AuthContextHolder.getUserId()));
    }

    @Operation(summary = "检查及扣减账户余额")
    @PostMapping("/checkAndDeduct")
    public Result checkAndDeduct(@RequestBody AccountLockVo accountDeductVo)
    {
        userAccountService.checkAndDeduct(accountDeductVo);
        return Result.ok();
    }

}

