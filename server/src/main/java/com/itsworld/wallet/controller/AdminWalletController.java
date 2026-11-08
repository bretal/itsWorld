package com.itsworld.wallet.controller;

import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.security.service.CurrentUserService;
import com.itsworld.wallet.dto.AdjustBalanceRequest;
import com.itsworld.wallet.dto.OfflineRechargeActionRequest;
import com.itsworld.wallet.dto.OfflineRechargeCreateRequest;
import com.itsworld.wallet.dto.RechargeOrderView;
import com.itsworld.wallet.dto.RechargePlanRequest;
import com.itsworld.wallet.dto.WalletAdjustmentView;
import com.itsworld.wallet.dto.WalletTransactionView;
import com.itsworld.wallet.entity.WalletRechargePlan;
import com.itsworld.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端钱包运营控制器（仅 ROLE_ADMIN）
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
public class AdminWalletController {

    private final WalletService walletService;
    private final CurrentUserService currentUserService;

    /**
     * 充值方案列表（含停用）
     */
    @GetMapping("/recharge-plans")
    public Result<List<WalletRechargePlan>> getPlans() {
        return Result.success(walletService.getAdminPlans());
    }

    /**
     * 新建充值方案
     */
    @PostMapping("/recharge-plans")
    public Result<WalletRechargePlan> createPlan(@Valid @RequestBody RechargePlanRequest request) {
        return Result.success(walletService.createPlan(request), "充值方案已创建");
    }

    /**
     * 更新充值方案
     */
    @PutMapping("/recharge-plans/{planId}")
    public Result<WalletRechargePlan> updatePlan(@PathVariable Long planId,
                                                 @Valid @RequestBody RechargePlanRequest request) {
        return Result.success(walletService.updatePlan(planId, request), "充值方案已更新");
    }

    /**
     * 下架充值方案
     */
    @DeleteMapping("/recharge-plans/{planId}")
    public Result<Void> disablePlan(@PathVariable Long planId) {
        walletService.disablePlan(planId);
        return Result.success(null, "充值方案已下架");
    }

    /**
     * 线下充值列表
     */
    @GetMapping("/offline-recharges")
    public Result<PageResult<RechargeOrderView>> getOfflineRecharges(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String status) {
        return Result.success(walletService.getOfflineRecharges(
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size), status));
    }

    /**
     * 创建线下充值申请
     */
    @PostMapping("/offline-recharges")
    public Result<RechargeOrderView> createOfflineRecharge(
            @Valid @RequestBody OfflineRechargeCreateRequest request) {
        Long operatorId = currentUserService.getUserId();
        return Result.success(walletService.createOfflineRecharge(operatorId, request), "申请已创建");
    }

    /**
     * 审批线下充值（确认收款并到账）
     */
    @PostMapping("/offline-recharges/{rechargeNo}/approve")
    public Result<RechargeOrderView> approveOfflineRecharge(
            @PathVariable String rechargeNo,
            @RequestBody(required = false) OfflineRechargeActionRequest request) {
        Long approverId = currentUserService.getUserId();
        String externalTxNo = request != null ? request.getExternalTxNo() : null;
        return Result.success(
                walletService.approveOfflineRecharge(approverId, rechargeNo, externalTxNo),
                "审批通过，已到账");
    }

    /**
     * 驳回线下充值
     */
    @PostMapping("/offline-recharges/{rechargeNo}/reject")
    public Result<RechargeOrderView> rejectOfflineRecharge(
            @PathVariable String rechargeNo,
            @RequestBody(required = false) OfflineRechargeActionRequest request) {
        Long approverId = currentUserService.getUserId();
        String rejectReason = request != null ? request.getRejectReason() : null;
        return Result.success(
                walletService.rejectOfflineRecharge(approverId, rechargeNo, rejectReason),
                "已驳回");
    }

    /**
     * 余额调增/调减
     */
    @PostMapping("/adjustments")
    public Result<WalletAdjustmentView> adjustBalance(
            @Valid @RequestBody AdjustBalanceRequest request) {
        Long operatorId = currentUserService.getUserId();
        return Result.success(walletService.adjustBalance(operatorId, request), "调账成功");
    }

    /**
     * 余额调账记录
     */
    @GetMapping("/adjustments")
    public Result<PageResult<WalletAdjustmentView>> getAdjustments(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Long userId) {
        return Result.success(walletService.getAdjustments(
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size), userId));
    }

    /**
     * 全部资金流水
     */
    @GetMapping("/transactions")
    public Result<PageResult<WalletTransactionView>> getTransactions(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String txType) {
        return Result.success(walletService.getAllTransactions(
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size), txType));
    }

}
