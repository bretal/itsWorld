package com.itsworld.commerce.order.controller;

import com.itsworld.commerce.order.dto.CheckoutPreviewRequest;
import com.itsworld.commerce.order.dto.CheckoutPreviewResponse;
import com.itsworld.commerce.order.dto.CheckoutSubmitRequest;
import com.itsworld.commerce.order.dto.OrderSubmitResponse;
import com.itsworld.commerce.order.service.CheckoutService;
import com.itsworld.common.result.Result;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结算控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CurrentUserService currentUserService;

    @PostMapping("/preview")
    public Result<CheckoutPreviewResponse> preview(@RequestBody(required = false) CheckoutPreviewRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(checkoutService.preview(userId,
                request != null ? request : new CheckoutPreviewRequest()));
    }

    @PostMapping("/submit")
    public Result<OrderSubmitResponse> submit(@Valid @RequestBody CheckoutSubmitRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(checkoutService.submit(userId, request), "订单已创建");
    }

}
