package com.itsworld.commerce.order.controller;

import com.itsworld.commerce.order.dto.OrderDetailView;
import com.itsworld.commerce.order.service.CommerceOrderService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户订单控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/orders")
@RequiredArgsConstructor
public class CommerceOrderController {

    private final CommerceOrderService orderService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<OrderDetailView>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        Long userId = currentUserService.getUserId();
        return Result.success(orderService.pageUserOrders(userId, status,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderDetailView> getOrder(@PathVariable String orderNo) {
        Long userId = currentUserService.getUserId();
        return Result.success(orderService.getUserOrder(userId, orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(@PathVariable String orderNo,
                                    @RequestParam(required = false) String reason) {
        Long userId = currentUserService.getUserId();
        orderService.cancelUserOrder(userId, orderNo, reason);
        return Result.success(null, "订单已取消");
    }

}
