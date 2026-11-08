package com.itsworld.commerce.order.controller;

import com.itsworld.commerce.order.dto.OrderDetailView;
import com.itsworld.commerce.order.service.CommerceOrderService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端订单控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin/commerce/orders")
@RequiredArgsConstructor
public class CommerceOrderAdminController {

    private final CommerceOrderService orderService;

    @GetMapping
    public Result<PageResult<OrderDetailView>> pageOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(orderService.pageAdminOrders(status, keyword,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderDetailView> getOrder(@PathVariable String orderNo) {
        return Result.success(orderService.getAdminOrder(orderNo));
    }

    @PostMapping("/{orderNo}/close")
    public Result<Void> closeOrder(@PathVariable String orderNo,
                                   @RequestParam(required = false) String reason) {
        orderService.closeAdminOrder(orderNo, reason);
        return Result.success(null, "订单已关闭");
    }

}
