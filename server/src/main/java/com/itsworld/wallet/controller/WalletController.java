package com.itsworld.wallet.controller;

import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.security.service.CurrentUserService;
import com.itsworld.wallet.dto.RechargeCreateRequest;
import com.itsworld.wallet.dto.RechargeOrderView;
import com.itsworld.wallet.dto.WalletSummaryResponse;
import com.itsworld.wallet.dto.WalletTransactionView;
import com.itsworld.wallet.entity.WalletRechargePlan;
import com.itsworld.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户钱包控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final CurrentUserService currentUserService;

    /**
     * 钱包余额摘要
     */
    @GetMapping
    public Result<WalletSummaryResponse> getWallet() {
        Long userId = currentUserService.getUserId();
        return Result.success(walletService.getSummary(userId));
    }

    /**
     * 启用中的充值方案
     */
    @GetMapping("/recharge-plans")
    public Result<List<WalletRechargePlan>> getRechargePlans() {
        return Result.success(walletService.getActivePlans());
    }

    /**
     * 我的充值单
     */
    @GetMapping("/recharges")
    public Result<PageResult<RechargeOrderView>> getRecharges(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        Long userId = currentUserService.getUserId();
        return Result.success(walletService.getUserRecharges(userId,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    /**
     * 创建线上充值单
     */
    @PostMapping("/recharges")
    public Result<RechargeOrderView> createRecharge(@Valid @RequestBody RechargeCreateRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(walletService.createRecharge(userId, request.getPlanId()), "充值单已创建");
    }

    /**
     * 查询充值单
     */
    @GetMapping("/recharges/{rechargeNo}")
    public Result<RechargeOrderView> getRecharge(@PathVariable String rechargeNo) {
        Long userId = currentUserService.getUserId();
        return Result.success(walletService.getRecharge(userId, rechargeNo));
    }

    /**
     * 开发环境模拟支付并到账
     */
    @PostMapping("/recharges/{rechargeNo}/mock-pay")
    public Result<RechargeOrderView> mockPay(@PathVariable String rechargeNo) {
        Long userId = currentUserService.getUserId();
        return Result.success(walletService.mockPay(userId, rechargeNo), "模拟支付成功");
    }

    /**
     * 资金流水
     */
    @GetMapping("/transactions")
    public Result<PageResult<WalletTransactionView>> getTransactions(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        Long userId = currentUserService.getUserId();
        return Result.success(walletService.getUserTransactions(userId,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

}
