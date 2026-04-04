package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoApiController
{

    @Autowired
    private OrderInfoService orderInfoService;

    @GuiGuLogin
    @Operation(summary = "确认订单")
    @PostMapping("trade")
    public Result<OrderInfoVo> trade(@RequestBody @Validated TradeVo tradeVo)
    {
        Long userId = AuthContextHolder.getUserId();
        OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo,
                                                         userId);
        return Result.ok(orderInfoVo);
    }

    @GuiGuLogin
    @Operation(summary = "提交订单")
    @PostMapping("submitOrder")
    public Result<Map<String, Object>> submitOrder(@RequestBody @Validated OrderInfoVo orderInfoVo)
    {
        Long userId = AuthContextHolder.getUserId();
        String orderNo = orderInfoService.submitOrder(orderInfoVo, userId);
        Map<String, Object> map = new HashMap();
        map.put("orderNo", orderNo);
        return Result.ok(map);
    }

    @GuiGuLogin
    @Operation(summary = "根据订单号获取订单信息")
    @GetMapping("getOrderInfo/{orderNo}")
    public Result<OrderInfo> getOrderInfo(@PathVariable String orderNo)
    {
        OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderNo(orderNo);
        return Result.ok(orderInfo);
    }

    @GuiGuLogin
    @Operation(summary = "获取用户订单")
    @GetMapping("findUserPage/{page}/{limit}")
    public Result index(
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,
            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit)
    {
        Long userId = AuthContextHolder.getUserId();
        Page<OrderInfo> pageParam = new Page<>(page, limit);
        IPage<OrderInfo> pageModel = orderInfoService.findUserPage(pageParam, userId);
        return Result.ok(pageModel);
    }
}
