package com.itsworld.commerce.order.controller;

import com.itsworld.commerce.order.dto.PaymentCreateRequest;
import com.itsworld.commerce.order.dto.PaymentResultView;
import com.itsworld.commerce.order.service.CommercePaymentService;
import com.itsworld.common.result.Result;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商业支付控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/orders")
@RequiredArgsConstructor
public class CommercePaymentController {

    private final CommercePaymentService paymentService;
    private final CurrentUserService currentUserService;

    @PostMapping("/{orderNo}/payments")
    public Result<PaymentResultView> createPayment(@PathVariable String orderNo,
                                                   @Valid @RequestBody PaymentCreateRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(paymentService.createPayment(userId, orderNo, request), "支付完成");
    }

    @GetMapping("/{orderNo}/payment-status")
    public Result<PaymentResultView> getPaymentStatus(@PathVariable String orderNo) {
        Long userId = currentUserService.getUserId();
        return Result.success(paymentService.getPaymentStatus(userId, orderNo));
    }

}
