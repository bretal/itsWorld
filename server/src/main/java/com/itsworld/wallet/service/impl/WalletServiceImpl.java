package com.itsworld.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.user.service.UserService;
import com.itsworld.wallet.dto.AdjustBalanceRequest;
import com.itsworld.wallet.dto.OfflineRechargeCreateRequest;
import com.itsworld.wallet.dto.RechargeOrderView;
import com.itsworld.wallet.dto.RechargePlanRequest;
import com.itsworld.wallet.dto.WalletAdjustmentView;
import com.itsworld.wallet.dto.WalletPaymentResult;
import com.itsworld.wallet.dto.WalletRefundResult;
import com.itsworld.wallet.dto.WalletSummaryResponse;
import com.itsworld.wallet.dto.WalletTransactionView;
import com.itsworld.wallet.entity.WalletAccount;
import com.itsworld.wallet.entity.WalletAdjustment;
import com.itsworld.wallet.entity.WalletFundBatch;
import com.itsworld.wallet.entity.WalletPaymentAllocation;
import com.itsworld.wallet.entity.WalletRechargeOrder;
import com.itsworld.wallet.entity.WalletRechargePlan;
import com.itsworld.wallet.entity.WalletRefund;
import com.itsworld.wallet.entity.WalletRefundAllocation;
import com.itsworld.wallet.entity.WalletTransaction;
import com.itsworld.wallet.entity.WalletTransactionDetail;
import com.itsworld.wallet.mapper.WalletAccountMapper;
import com.itsworld.wallet.mapper.WalletAdjustmentMapper;
import com.itsworld.wallet.mapper.WalletFundBatchMapper;
import com.itsworld.wallet.mapper.WalletPaymentAllocationMapper;
import com.itsworld.wallet.mapper.WalletRechargeOrderMapper;
import com.itsworld.wallet.mapper.WalletRechargePlanMapper;
import com.itsworld.wallet.mapper.WalletRefundAllocationMapper;
import com.itsworld.wallet.mapper.WalletRefundMapper;
import com.itsworld.wallet.mapper.WalletTransactionDetailMapper;
import com.itsworld.wallet.mapper.WalletTransactionMapper;
import com.itsworld.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 钱包业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final String CHANNEL_ONLINE = "ONLINE";
    private static final String CHANNEL_OFFLINE = "OFFLINE";
    private static final String PAYMENT_CREATED = "CREATED";
    private static final String PAYMENT_PAID = "PAID";
    private static final String PAYMENT_CLOSED = "CLOSED";
    private static final String PAYMENT_EXCEPTION = "EXCEPTION";
    private static final String CREDIT_PENDING = "PENDING";
    private static final String CREDIT_CREDITED = "CREDITED";
    private static final String FUND_PRINCIPAL = "PRINCIPAL";
    private static final String FUND_BONUS = "BONUS";

    private final WalletAccountMapper walletAccountMapper;
    private final WalletFundBatchMapper walletFundBatchMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WalletTransactionDetailMapper walletTransactionDetailMapper;
    private final WalletRechargeOrderMapper walletRechargeOrderMapper;
    private final WalletRechargePlanMapper walletRechargePlanMapper;
    private final WalletAdjustmentMapper walletAdjustmentMapper;
    private final WalletPaymentAllocationMapper walletPaymentAllocationMapper;
    private final WalletRefundMapper walletRefundMapper;
    private final WalletRefundAllocationMapper walletRefundAllocationMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.mock-enabled:false}")
    private boolean mockPayEnabled;

    @Override
    public WalletSummaryResponse getSummary(Long userId) {
        return buildSummary(getOrCreateAccount(userId));
    }

    @Override
    public List<WalletRechargePlan> getActivePlans() {
        return walletRechargePlanMapper.selectList(new LambdaQueryWrapper<WalletRechargePlan>()
                .eq(WalletRechargePlan::getStatus, 1)
                .orderByAsc(WalletRechargePlan::getSortOrder)
                .orderByAsc(WalletRechargePlan::getId));
    }

    @Override
    public PageResult<RechargeOrderView> getUserRecharges(Long userId, long pageNum, long pageSize) {
        Page<RechargeOrderView> page = new Page<>(pageNum, pageSize);
        Page<RechargeOrderView> result = walletRechargeOrderMapper.selectViewPage(page, userId, null, null);
        applyMockFlag(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public RechargeOrderView createRecharge(Long userId, Long planId) {
        WalletRechargePlan plan = getActivePlan(planId);
        String rechargeNo = generateNo("RC");
        WalletRechargeOrder order = WalletRechargeOrder.builder()
                .rechargeNo(rechargeNo)
                .userId(userId)
                .planId(plan.getId())
                .principalAmount(plan.getPrincipalAmount())
                .bonusAmount(plan.getBonusAmount())
                .channel(CHANNEL_ONLINE)
                .paymentMethod("MOCK")
                .paymentStatus(PAYMENT_CREATED)
                .creditStatus(CREDIT_PENDING)
                .planSnapshot(buildPlanSnapshot(plan))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        walletRechargeOrderMapper.insert(order);
        log.info("线上充值单创建: rechargeNo={}, userId={}, planId={}", rechargeNo, userId, planId);
        return requireView(rechargeNo);
    }

    @Override
    public RechargeOrderView getRecharge(Long userId, String rechargeNo) {
        WalletRechargeOrder order = walletRechargeOrderMapper.selectByRechargeNo(rechargeNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_NOT_FOUND);
        }
        return requireView(rechargeNo);
    }

    @Override
    @Transactional
    public RechargeOrderView mockPay(Long userId, String rechargeNo) {
        if (!mockPayEnabled) {
            throw new BusinessException(ErrorCode.MOCK_PAY_DISABLED);
        }
        WalletRechargeOrder order = walletRechargeOrderMapper.selectByRechargeNoForUpdate(rechargeNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_NOT_FOUND);
        }
        if (!CHANNEL_ONLINE.equals(order.getChannel())) {
            throw new BusinessException(ErrorCode.RECHARGE_STATUS_ERROR);
        }
        if (CREDIT_CREDITED.equals(order.getCreditStatus())) {
            return requireView(rechargeNo);
        }
        if (PAYMENT_CLOSED.equals(order.getPaymentStatus())
                || PAYMENT_EXCEPTION.equals(order.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.RECHARGE_STATUS_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        order.setPaymentStatus(PAYMENT_PAID);
        order.setPaidAt(now);
        order.setExternalTxNo(order.getExternalTxNo() != null ? order.getExternalTxNo() : "MOCK-" + rechargeNo);
        order.setUpdatedAt(now);
        walletRechargeOrderMapper.updateById(order);

        creditLocked(order);
        log.info("模拟支付到账: rechargeNo={}, userId={}", rechargeNo, userId);
        return requireView(rechargeNo);
    }

    @Override
    public PageResult<WalletTransactionView> getUserTransactions(Long userId, long pageNum, long pageSize) {
        Page<WalletTransactionView> page = new Page<>(pageNum, pageSize);
        Page<WalletTransactionView> result = walletTransactionMapper.selectViewPage(page, userId, null);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public WalletPaymentResult payForBusinessOrder(Long userId, Long paymentLinkId, String orderNo,
                                                   long amount, String idempotencyKey) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "支付金额必须大于0");
        }
        if (paymentLinkId == null || orderNo == null || idempotencyKey == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "支付参数不完整");
        }
        WalletTransaction existing = walletTransactionMapper.selectOne(
                new LambdaQueryWrapper<WalletTransaction>()
                        .eq(WalletTransaction::getIdempotencyKey, idempotencyKey)
                        .last("LIMIT 1"));
        if (existing != null) {
            long paid = Math.abs(nvl(existing.getPrincipalDelta())) + Math.abs(nvl(existing.getBonusDelta()));
            return WalletPaymentResult.builder()
                    .txNo(existing.getTxNo())
                    .paidAmount(paid)
                    .principalDelta(existing.getPrincipalDelta())
                    .bonusDelta(existing.getBonusDelta())
                    .build();
        }

        getOrCreateAccount(userId);
        WalletAccount locked = walletAccountMapper.selectByUserIdForUpdate(userId);
        if (locked == null) {
            throw new BusinessException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND);
        }
        long beforePrincipal = nvl(locked.getPrincipalBalance());
        long beforeBonus = nvl(locked.getBonusBalance());
        long frozenPrincipal = nvl(locked.getPrincipalFrozen());
        long frozenBonus = nvl(locked.getBonusFrozen());
        long available = beforePrincipal + beforeBonus - frozenPrincipal - frozenBonus;
        if (available < amount) {
            throw new BusinessException(ErrorCode.WALLET_BALANCE_INSUFFICIENT);
        }

        List<WalletFundBatch> batches = walletFundBatchMapper.selectList(
                new LambdaQueryWrapper<WalletFundBatch>()
                        .eq(WalletFundBatch::getWalletId, locked.getId())
                        .eq(WalletFundBatch::getStatus, 1));
        batches.sort(this::compareForPayment);

        List<BatchPayment> payments = new ArrayList<>();
        long remaining = amount;
        long principalDeduct = 0L;
        long bonusDeduct = 0L;
        for (WalletFundBatch batch : batches) {
            if (remaining <= 0) {
                break;
            }
            long batchAvailable = Math.max(0L,
                    nvl(batch.getRemainingAmount()) - nvl(batch.getFrozenAmount()));
            if (batchAvailable <= 0) {
                continue;
            }
            long deduct = Math.min(batchAvailable, remaining);
            payments.add(new BatchPayment(batch, deduct));
            if (FUND_BONUS.equals(batch.getFundType())) {
                bonusDeduct += deduct;
            } else {
                principalDeduct += deduct;
            }
            remaining -= deduct;
        }
        if (remaining > 0) {
            throw new BusinessException(ErrorCode.WALLET_BALANCE_INSUFFICIENT);
        }

        LocalDateTime now = LocalDateTime.now();
        WalletTransaction transaction = WalletTransaction.builder()
                .txNo(generateNo("WT"))
                .walletId(locked.getId())
                .userId(userId)
                .txType("PAYMENT")
                .businessType("COMMERCE_ORDER")
                .businessNo(orderNo)
                .idempotencyKey(idempotencyKey)
                .principalDelta(-principalDeduct)
                .bonusDelta(-bonusDeduct)
                .principalBefore(beforePrincipal)
                .principalAfter(beforePrincipal - principalDeduct)
                .bonusBefore(beforeBonus)
                .bonusAfter(beforeBonus - bonusDeduct)
                .remark("订单支付：" + orderNo)
                .createdAt(now)
                .build();
        walletTransactionMapper.insert(transaction);

        for (BatchPayment payment : payments) {
            WalletFundBatch batch = payment.batch();
            batch.setRemainingAmount(nvl(batch.getRemainingAmount()) - payment.amount());
            if (batch.getRemainingAmount() == 0L) {
                batch.setStatus(0);
            }
            walletFundBatchMapper.updateById(batch);
            walletTransactionDetailMapper.insert(WalletTransactionDetail.builder()
                    .transactionId(transaction.getId())
                    .batchId(batch.getId())
                    .fundType(batch.getFundType())
                    .amountDelta(-payment.amount())
                    .frozenDelta(0L)
                    .createdAt(now)
                    .build());
            walletPaymentAllocationMapper.insert(WalletPaymentAllocation.builder()
                    .payOrderId(paymentLinkId)
                    .walletTxId(transaction.getId())
                    .batchId(batch.getId())
                    .fundType(batch.getFundType())
                    .paidAmount(payment.amount())
                    .refundedAmount(0L)
                    .createdAt(now)
                    .build());
        }

        locked.setPrincipalBalance(beforePrincipal - principalDeduct);
        locked.setBonusBalance(beforeBonus - bonusDeduct);
        locked.setVersion(locked.getVersion() != null ? locked.getVersion() + 1 : 1);
        locked.setUpdatedAt(now);
        walletAccountMapper.updateById(locked);

        log.info("订单钱包支付: orderNo={}, userId={}, amount={}, principal={}, bonus={}",
                orderNo, userId, amount, principalDeduct, bonusDeduct);
        return WalletPaymentResult.builder()
                .txNo(transaction.getTxNo())
                .paidAmount(amount)
                .principalDelta(-principalDeduct)
                .bonusDelta(-bonusDeduct)
                .build();
    }

    @Override
    @Transactional
    public WalletRefundResult refundBusinessPayment(Long userId, Long paymentLinkId, String orderNo,
                                                    long amount, String reason, String refundNo) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "退款金额必须大于0");
        }
        if (paymentLinkId == null || orderNo == null || refundNo == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "退款参数不完整");
        }
        WalletRefund existing = walletRefundMapper.selectByRefundNo(refundNo);
        if (existing != null) {
            return WalletRefundResult.builder()
                    .refundNo(existing.getRefundNo())
                    .refundedAmount(existing.getAmount())
                    .status(existing.getStatus())
                    .build();
        }
        WalletAccount locked = walletAccountMapper.selectByUserIdForUpdate(userId);
        if (locked == null) {
            throw new BusinessException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND);
        }
        List<WalletPaymentAllocation> allocations =
                walletPaymentAllocationMapper.selectByPayOrderId(paymentLinkId);
        if (allocations.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(),
                    "该支付没有钱包分摊，无法执行钱包退款");
        }
        long refundable = allocations.stream()
                .mapToLong(item -> nvl(item.getPaidAmount()) - nvl(item.getRefundedAmount()))
                .sum();
        if (amount > refundable) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(),
                    "退款金额超过可退金额 " + refundable);
        }

        List<RefundAllocationPlan> plans = new ArrayList<>();
        long remaining = amount;
        long principalRestored = 0L;
        long bonusRestored = 0L;
        for (WalletPaymentAllocation allocation : allocations) {
            if (remaining <= 0) {
                break;
            }
            long allocationRefundable = nvl(allocation.getPaidAmount())
                    - nvl(allocation.getRefundedAmount());
            if (allocationRefundable <= 0) {
                continue;
            }
            long refund = Math.min(allocationRefundable, remaining);
            WalletFundBatch batch = walletFundBatchMapper.selectById(allocation.getBatchId());
            if (batch == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "原支付批次不存在");
            }
            plans.add(new RefundAllocationPlan(allocation, batch, refund));
            if (FUND_BONUS.equals(allocation.getFundType())) {
                bonusRestored += refund;
            } else {
                principalRestored += refund;
            }
            remaining -= refund;
        }
        if (remaining > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "退款分摊失败");
        }

        LocalDateTime now = LocalDateTime.now();
        long beforePrincipal = nvl(locked.getPrincipalBalance());
        long beforeBonus = nvl(locked.getBonusBalance());
        WalletTransaction transaction = WalletTransaction.builder()
                .txNo(generateNo("WT"))
                .walletId(locked.getId())
                .userId(userId)
                .txType("REFUND")
                .businessType("COMMERCE_ORDER")
                .businessNo(orderNo)
                .idempotencyKey("COMMERCE_REFUND:" + refundNo)
                .principalDelta(principalRestored)
                .bonusDelta(bonusRestored)
                .principalBefore(beforePrincipal)
                .principalAfter(beforePrincipal + principalRestored)
                .bonusBefore(beforeBonus)
                .bonusAfter(beforeBonus + bonusRestored)
                .remark("订单退款：" + orderNo)
                .createdAt(now)
                .build();
        walletTransactionMapper.insert(transaction);

        WalletRefund refund = WalletRefund.builder()
                .refundNo(refundNo)
                .payOrderId(paymentLinkId)
                .businessOrderNo(orderNo)
                .amount(amount)
                .status("REFUNDED")
                .reason(reason)
                .createdAt(now)
                .updatedAt(now)
                .build();
        walletRefundMapper.insert(refund);

        for (RefundAllocationPlan plan : plans) {
            WalletPaymentAllocation allocation = plan.allocation();
            allocation.setRefundedAmount(nvl(allocation.getRefundedAmount()) + plan.amount());
            walletPaymentAllocationMapper.updateById(allocation);

            WalletFundBatch batch = plan.batch();
            batch.setRemainingAmount(nvl(batch.getRemainingAmount()) + plan.amount());
            batch.setStatus(1);
            walletFundBatchMapper.updateById(batch);

            walletTransactionDetailMapper.insert(WalletTransactionDetail.builder()
                    .transactionId(transaction.getId())
                    .batchId(batch.getId())
                    .fundType(batch.getFundType())
                    .amountDelta(plan.amount())
                    .frozenDelta(0L)
                    .createdAt(now)
                    .build());
            walletRefundAllocationMapper.insert(WalletRefundAllocation.builder()
                    .refundId(refund.getId())
                    .originalPaymentAllocationId(allocation.getId())
                    .amount(plan.amount())
                    .restoredBatchId(batch.getId())
                    .createdAt(now)
                    .build());
        }

        locked.setPrincipalBalance(beforePrincipal + principalRestored);
        locked.setBonusBalance(beforeBonus + bonusRestored);
        locked.setVersion(locked.getVersion() != null ? locked.getVersion() + 1 : 1);
        locked.setUpdatedAt(now);
        walletAccountMapper.updateById(locked);

        log.info("订单钱包退款: orderNo={}, userId={}, amount={}, principal={}, bonus={}",
                orderNo, userId, amount, principalRestored, bonusRestored);
        return WalletRefundResult.builder()
                .refundNo(refundNo)
                .refundedAmount(amount)
                .principalRestored(principalRestored)
                .bonusRestored(bonusRestored)
                .status("REFUNDED")
                .build();
    }

    private record RefundAllocationPlan(WalletPaymentAllocation allocation,
                                        WalletFundBatch batch, long amount) {
    }

    private int compareForPayment(WalletFundBatch a, WalletFundBatch b) {
        int aType = FUND_BONUS.equals(a.getFundType()) ? 0 : 1;
        int bType = FUND_BONUS.equals(b.getFundType()) ? 0 : 1;
        if (aType != bType) {
            return Integer.compare(aType, bType);
        }
        if (aType == 0) {
            int compareExpire = Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())
                    .compare(a.getExpireAt(), b.getExpireAt());
            if (compareExpire != 0) {
                return compareExpire;
            }
        }
        int compareCreated = Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())
                .compare(a.getCreatedAt(), b.getCreatedAt());
        if (compareCreated != 0) {
            return compareCreated;
        }
        return Comparator.nullsLast(Comparator.<Long>naturalOrder()).compare(a.getId(), b.getId());
    }

    private record BatchPayment(WalletFundBatch batch, long amount) {
    }

    @Override
    public List<WalletRechargePlan> getAdminPlans() {
        return walletRechargePlanMapper.selectList(new LambdaQueryWrapper<WalletRechargePlan>()
                .orderByAsc(WalletRechargePlan::getSortOrder)
                .orderByAsc(WalletRechargePlan::getId));
    }

    @Override
    @Transactional
    public WalletRechargePlan createPlan(RechargePlanRequest request) {
        validatePlan(request);
        WalletRechargePlan plan = WalletRechargePlan.builder()
                .planName(request.getPlanName().trim())
                .principalAmount(request.getPrincipalAmount())
                .bonusAmount(request.getBonusAmount() != null ? request.getBonusAmount() : 0L)
                .bonusValidDays(request.getBonusValidDays() != null ? request.getBonusValidDays() : 0)
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        walletRechargePlanMapper.insert(plan);
        log.info("充值方案创建: planId={}, name={}", plan.getId(), plan.getPlanName());
        return plan;
    }

    @Override
    @Transactional
    public WalletRechargePlan updatePlan(Long planId, RechargePlanRequest request) {
        WalletRechargePlan plan = walletRechargePlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.RECHARGE_PLAN_NOT_FOUND);
        }
        validatePlan(request);
        plan.setPlanName(request.getPlanName().trim());
        plan.setPrincipalAmount(request.getPrincipalAmount());
        plan.setBonusAmount(request.getBonusAmount() != null ? request.getBonusAmount() : 0L);
        plan.setBonusValidDays(request.getBonusValidDays() != null ? request.getBonusValidDays() : 0);
        plan.setDescription(request.getDescription());
        plan.setStatus(request.getStatus() != null ? request.getStatus() : plan.getStatus());
        plan.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : plan.getSortOrder());
        plan.setUpdatedAt(LocalDateTime.now());
        walletRechargePlanMapper.updateById(plan);
        log.info("充值方案更新: planId={}, name={}", planId, plan.getPlanName());
        return plan;
    }

    @Override
    @Transactional
    public void disablePlan(Long planId) {
        WalletRechargePlan plan = walletRechargePlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.RECHARGE_PLAN_NOT_FOUND);
        }
        plan.setStatus(0);
        plan.setUpdatedAt(LocalDateTime.now());
        walletRechargePlanMapper.updateById(plan);
        log.info("充值方案下架: planId={}", planId);
    }

    @Override
    public PageResult<RechargeOrderView> getOfflineRecharges(long pageNum, long pageSize, String paymentStatus) {
        Page<RechargeOrderView> page = new Page<>(pageNum, pageSize);
        Page<RechargeOrderView> result = walletRechargeOrderMapper.selectViewPage(
                page, null, CHANNEL_OFFLINE, paymentStatus);
        applyMockFlag(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public RechargeOrderView createOfflineRecharge(Long operatorId, OfflineRechargeCreateRequest request) {
        userService.getUserById(request.getUserId());
        WalletRechargePlan plan = getActivePlan(request.getPlanId());
        String rechargeNo = generateNo("OF");
        WalletRechargeOrder order = WalletRechargeOrder.builder()
                .rechargeNo(rechargeNo)
                .userId(request.getUserId())
                .planId(plan.getId())
                .principalAmount(plan.getPrincipalAmount())
                .bonusAmount(plan.getBonusAmount())
                .channel(CHANNEL_OFFLINE)
                .paymentMethod("OFFLINE")
                .storeName(request.getStoreName().trim())
                .operatorId(operatorId)
                .paymentStatus(PAYMENT_CREATED)
                .creditStatus(CREDIT_PENDING)
                .planSnapshot(buildPlanSnapshot(plan))
                .remark(request.getRemark())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        walletRechargeOrderMapper.insert(order);
        log.info("线下充值申请创建: rechargeNo={}, userId={}, operatorId={}",
                rechargeNo, request.getUserId(), operatorId);
        return requireView(rechargeNo);
    }

    @Override
    @Transactional
    public RechargeOrderView approveOfflineRecharge(Long approverId, String rechargeNo, String externalTxNo) {
        WalletRechargeOrder order = walletRechargeOrderMapper.selectByRechargeNoForUpdate(rechargeNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_NOT_FOUND);
        }
        if (!CHANNEL_OFFLINE.equals(order.getChannel())) {
            throw new BusinessException(ErrorCode.RECHARGE_STATUS_ERROR);
        }
        if (CREDIT_CREDITED.equals(order.getCreditStatus())) {
            return requireView(rechargeNo);
        }
        if (PAYMENT_CLOSED.equals(order.getPaymentStatus())
                || PAYMENT_EXCEPTION.equals(order.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.RECHARGE_STATUS_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        order.setPaymentStatus(PAYMENT_PAID);
        order.setPaidAt(now);
        order.setApproverId(approverId);
        order.setApprovedAt(now);
        order.setExternalTxNo(externalTxNo);
        order.setUpdatedAt(now);
        walletRechargeOrderMapper.updateById(order);

        creditLocked(order);
        log.info("线下充值审批到账: rechargeNo={}, approverId={}", rechargeNo, approverId);
        return requireView(rechargeNo);
    }

    @Override
    @Transactional
    public RechargeOrderView rejectOfflineRecharge(Long approverId, String rechargeNo, String rejectReason) {
        WalletRechargeOrder order = walletRechargeOrderMapper.selectByRechargeNoForUpdate(rechargeNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_NOT_FOUND);
        }
        if (!CHANNEL_OFFLINE.equals(order.getChannel())) {
            throw new BusinessException(ErrorCode.RECHARGE_STATUS_ERROR);
        }
        if (CREDIT_CREDITED.equals(order.getCreditStatus())) {
            throw new BusinessException(ErrorCode.RECHARGE_ALREADY_CREDITED);
        }
        if (PAYMENT_CLOSED.equals(order.getPaymentStatus())) {
            return requireView(rechargeNo);
        }

        LocalDateTime now = LocalDateTime.now();
        order.setPaymentStatus(PAYMENT_CLOSED);
        order.setApproverId(approverId);
        order.setApprovedAt(now);
        order.setRejectReason(rejectReason != null && !rejectReason.isBlank()
                ? rejectReason.trim() : "管理员驳回");
        order.setUpdatedAt(now);
        walletRechargeOrderMapper.updateById(order);
        log.info("线下充值驳回: rechargeNo={}, approverId={}", rechargeNo, approverId);
        return requireView(rechargeNo);
    }

    @Override
    public PageResult<WalletTransactionView> getAllTransactions(long pageNum, long pageSize, String txType) {
        Page<WalletTransactionView> page = new Page<>(pageNum, pageSize);
        Page<WalletTransactionView> result = walletTransactionMapper.selectViewPage(page, null, txType);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public WalletAdjustmentView adjustBalance(Long operatorId, AdjustBalanceRequest request) {
        if (request.getUserId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "用户ID不能为空");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "调账原因不能为空");
        }
        long principalDelta = nvl(request.getPrincipalDelta());
        long bonusDelta = nvl(request.getBonusDelta());
        if (principalDelta == 0 && bonusDelta == 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "调账金额不能为0");
        }

        userService.getUserById(request.getUserId());
        getOrCreateAccount(request.getUserId());
        WalletAccount locked = walletAccountMapper.selectByUserIdForUpdate(request.getUserId());
        if (locked == null) {
            throw new BusinessException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND);
        }

        long beforePrincipal = nvl(locked.getPrincipalBalance());
        long beforeBonus = nvl(locked.getBonusBalance());
        long afterPrincipal = beforePrincipal + principalDelta;
        long afterBonus = beforeBonus + bonusDelta;
        long principalFrozen = nvl(locked.getPrincipalFrozen());
        long bonusFrozen = nvl(locked.getBonusFrozen());
        if (afterPrincipal < 0 || afterBonus < 0
                || afterPrincipal < principalFrozen || afterBonus < bonusFrozen) {
            throw new BusinessException(ErrorCode.WALLET_BALANCE_INSUFFICIENT);
        }

        LocalDateTime now = LocalDateTime.now();
        String reason = request.getReason().trim();
        if (reason.length() > 255) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "调账原因不能超过255个字符");
        }
        String adjustmentNo = generateNo("ADJ");
        WalletAdjustment adjustment = WalletAdjustment.builder()
                .adjustmentNo(adjustmentNo)
                .userId(request.getUserId())
                .principalDelta(principalDelta)
                .bonusDelta(bonusDelta)
                .principalBefore(beforePrincipal)
                .principalAfter(afterPrincipal)
                .bonusBefore(beforeBonus)
                .bonusAfter(afterBonus)
                .reason(reason)
                .operatorId(operatorId)
                .status("SUCCESS")
                .createdAt(now)
                .build();
        walletAdjustmentMapper.insert(adjustment);

        WalletTransaction transaction = WalletTransaction.builder()
                .txNo(generateNo("WT"))
                .walletId(locked.getId())
                .userId(request.getUserId())
                .txType("ADJUSTMENT")
                .businessType("WALLET_ADJUSTMENT")
                .businessNo(adjustmentNo)
                .idempotencyKey("ADJUSTMENT:" + adjustmentNo)
                .principalDelta(principalDelta)
                .bonusDelta(bonusDelta)
                .principalBefore(beforePrincipal)
                .principalAfter(afterPrincipal)
                .bonusBefore(beforeBonus)
                .bonusAfter(afterBonus)
                .remark("余额调账：" + reason)
                .createdAt(now)
                .build();
        walletTransactionMapper.insert(transaction);

        if (principalDelta > 0) {
            createAdjustmentBatch(locked.getId(), FUND_PRINCIPAL, principalDelta, now, transaction.getId());
        } else if (principalDelta < 0) {
            deductAdjustmentBatches(locked.getId(), FUND_PRINCIPAL, -principalDelta, now, transaction.getId());
        }
        if (bonusDelta > 0) {
            createAdjustmentBatch(locked.getId(), FUND_BONUS, bonusDelta, now, transaction.getId());
        } else if (bonusDelta < 0) {
            deductAdjustmentBatches(locked.getId(), FUND_BONUS, -bonusDelta, now, transaction.getId());
        }

        locked.setPrincipalBalance(afterPrincipal);
        locked.setBonusBalance(afterBonus);
        locked.setVersion(locked.getVersion() != null ? locked.getVersion() + 1 : 1);
        locked.setUpdatedAt(now);
        walletAccountMapper.updateById(locked);

        log.info("余额调账: adjustmentNo={}, userId={}, principalDelta={}, bonusDelta={}, operatorId={}",
                adjustmentNo, request.getUserId(), principalDelta, bonusDelta, operatorId);
        WalletAdjustmentView view = walletAdjustmentMapper.selectViewByNo(adjustmentNo);
        if (view == null) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
        return view;
    }

    @Override
    public PageResult<WalletAdjustmentView> getAdjustments(long pageNum, long pageSize, Long userId) {
        Page<WalletAdjustmentView> page = new Page<>(pageNum, pageSize);
        Page<WalletAdjustmentView> result = walletAdjustmentMapper.selectViewPage(page, userId);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 正数调账创建资金批次并写明细
     */
    private void createAdjustmentBatch(Long walletId, String fundType, long amount,
                                       LocalDateTime now, Long transactionId) {
        WalletFundBatch batch = WalletFundBatch.builder()
                .walletId(walletId)
                .rechargeOrderId(null)
                .fundType(fundType)
                .originalAmount(amount)
                .remainingAmount(amount)
                .frozenAmount(0L)
                .expireAt(null)
                .scope("ALL")
                .status(1)
                .createdAt(now)
                .build();
        walletFundBatchMapper.insert(batch);
        walletTransactionDetailMapper.insert(WalletTransactionDetail.builder()
                .transactionId(transactionId)
                .batchId(batch.getId())
                .fundType(fundType)
                .amountDelta(amount)
                .frozenDelta(0L)
                .createdAt(now)
                .build());
    }

    /**
     * 负数调账从可用批次扣减；冻结金额不参与扣减。
     */
    private void deductAdjustmentBatches(Long walletId, String fundType, long amount,
                                         LocalDateTime now, Long transactionId) {
        List<WalletFundBatch> batches = walletFundBatchMapper.selectList(
                new LambdaQueryWrapper<WalletFundBatch>()
                        .eq(WalletFundBatch::getWalletId, walletId)
                        .eq(WalletFundBatch::getFundType, fundType)
                        .eq(WalletFundBatch::getStatus, 1));
        Comparator<WalletFundBatch> comparator = Comparator
                .comparing((WalletFundBatch batch) ->
                                FUND_BONUS.equals(fundType) ? batch.getExpireAt() : batch.getCreatedAt(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WalletFundBatch::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WalletFundBatch::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
        batches.sort(comparator);

        long available = 0L;
        for (WalletFundBatch batch : batches) {
            available += Math.max(0L,
                    nvl(batch.getRemainingAmount()) - nvl(batch.getFrozenAmount()));
        }
        if (available < amount) {
            throw new BusinessException(ErrorCode.WALLET_BALANCE_INSUFFICIENT);
        }

        long remaining = amount;
        for (WalletFundBatch batch : batches) {
            if (remaining <= 0) {
                break;
            }
            long batchAvailable = Math.max(0L,
                    nvl(batch.getRemainingAmount()) - nvl(batch.getFrozenAmount()));
            if (batchAvailable <= 0) {
                continue;
            }
            long deduct = Math.min(batchAvailable, remaining);
            batch.setRemainingAmount(nvl(batch.getRemainingAmount()) - deduct);
            if (batch.getRemainingAmount() == 0L) {
                batch.setStatus(0);
            }
            walletFundBatchMapper.updateById(batch);
            walletTransactionDetailMapper.insert(WalletTransactionDetail.builder()
                    .transactionId(transactionId)
                    .batchId(batch.getId())
                    .fundType(fundType)
                    .amountDelta(-deduct)
                    .frozenDelta(0L)
                    .createdAt(now)
                    .build());
            remaining -= deduct;
        }
        if (remaining > 0) {
            throw new BusinessException(ErrorCode.WALLET_BALANCE_INSUFFICIENT);
        }
    }

    /**
     * 充值单已加行锁，确认已支付后在同一事务内入账（幂等）。
     */
    private void creditLocked(WalletRechargeOrder order) {
        if (CREDIT_CREDITED.equals(order.getCreditStatus())) {
            return;
        }
        if (!PAYMENT_PAID.equals(order.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.RECHARGE_STATUS_ERROR);
        }

        WalletAccount account = getOrCreateAccount(order.getUserId());
        WalletAccount locked = walletAccountMapper.selectByUserIdForUpdate(order.getUserId());
        if (locked == null) {
            throw new BusinessException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND);
        }

        long principal = order.getPrincipalAmount() != null ? order.getPrincipalAmount() : 0L;
        long bonus = order.getBonusAmount() != null ? order.getBonusAmount() : 0L;
        long beforePrincipal = nvl(locked.getPrincipalBalance());
        long beforeBonus = nvl(locked.getBonusBalance());
        LocalDateTime now = LocalDateTime.now();

        WalletFundBatch principalBatch = WalletFundBatch.builder()
                .walletId(locked.getId())
                .rechargeOrderId(order.getId())
                .fundType(FUND_PRINCIPAL)
                .originalAmount(principal)
                .remainingAmount(principal)
                .frozenAmount(0L)
                .expireAt(null)
                .scope("ALL")
                .status(1)
                .createdAt(now)
                .build();
        walletFundBatchMapper.insert(principalBatch);

        WalletFundBatch bonusBatch = null;
        if (bonus > 0) {
            int bonusValidDays = getBonusValidDays(order);
            LocalDateTime expireAt = bonusValidDays > 0 ? now.plusDays(bonusValidDays) : null;
            bonusBatch = WalletFundBatch.builder()
                    .walletId(locked.getId())
                    .rechargeOrderId(order.getId())
                    .fundType(FUND_BONUS)
                    .originalAmount(bonus)
                    .remainingAmount(bonus)
                    .frozenAmount(0L)
                    .expireAt(expireAt)
                    .scope("ALL")
                    .status(1)
                    .createdAt(now)
                    .build();
            walletFundBatchMapper.insert(bonusBatch);
        }

        WalletTransaction transaction = WalletTransaction.builder()
                .txNo(generateNo("WT"))
                .walletId(locked.getId())
                .userId(order.getUserId())
                .txType("RECHARGE")
                .businessType("RECHARGE_ORDER")
                .businessNo(order.getRechargeNo())
                .idempotencyKey("RECHARGE_CREDIT:" + order.getRechargeNo())
                .principalDelta(principal)
                .bonusDelta(bonus)
                .principalBefore(beforePrincipal)
                .principalAfter(beforePrincipal + principal)
                .bonusBefore(beforeBonus)
                .bonusAfter(beforeBonus + bonus)
                .remark("充值到账")
                .createdAt(now)
                .build();
        walletTransactionMapper.insert(transaction);

        walletTransactionDetailMapper.insert(WalletTransactionDetail.builder()
                .transactionId(transaction.getId())
                .batchId(principalBatch.getId())
                .fundType(FUND_PRINCIPAL)
                .amountDelta(principal)
                .frozenDelta(0L)
                .createdAt(now)
                .build());
        if (bonusBatch != null) {
            walletTransactionDetailMapper.insert(WalletTransactionDetail.builder()
                    .transactionId(transaction.getId())
                    .batchId(bonusBatch.getId())
                    .fundType(FUND_BONUS)
                    .amountDelta(bonus)
                    .frozenDelta(0L)
                    .createdAt(now)
                    .build());
        }

        locked.setPrincipalBalance(beforePrincipal + principal);
        locked.setBonusBalance(beforeBonus + bonus);
        locked.setVersion(locked.getVersion() != null ? locked.getVersion() + 1 : 1);
        locked.setUpdatedAt(now);
        walletAccountMapper.updateById(locked);

        order.setCreditStatus(CREDIT_CREDITED);
        order.setCreditedAt(now);
        order.setUpdatedAt(now);
        walletRechargeOrderMapper.updateById(order);

        log.info("钱包充值入账: rechargeNo={}, userId={}, principal={}, bonus={}",
                order.getRechargeNo(), order.getUserId(), principal, bonus);
    }

    private WalletAccount getOrCreateAccount(Long userId) {
        WalletAccount account = walletAccountMapper.selectByUserId(userId);
        if (account != null) {
            return account;
        }
        account = WalletAccount.builder()
                .userId(userId)
                .principalBalance(0L)
                .bonusBalance(0L)
                .principalFrozen(0L)
                .bonusFrozen(0L)
                .status(1)
                .version(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            walletAccountMapper.insert(account);
        } catch (DuplicateKeyException e) {
            log.info("钱包账户已由其他请求创建: userId={}", userId);
            account = walletAccountMapper.selectByUserId(userId);
        }
        if (account == null) {
            throw new BusinessException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND);
        }
        return account;
    }

    private WalletSummaryResponse buildSummary(WalletAccount account) {
        long principal = nvl(account.getPrincipalBalance());
        long bonus = nvl(account.getBonusBalance());
        long principalFrozen = nvl(account.getPrincipalFrozen());
        long bonusFrozen = nvl(account.getBonusFrozen());
        return WalletSummaryResponse.builder()
                .principalBalance(principal)
                .bonusBalance(bonus)
                .principalFrozen(principalFrozen)
                .bonusFrozen(bonusFrozen)
                .availableBalance(principal + bonus - principalFrozen - bonusFrozen)
                .totalBalance(principal + bonus)
                .mockPayEnabled(mockPayEnabled)
                .build();
    }

    private WalletRechargePlan getActivePlan(Long planId) {
        WalletRechargePlan plan = walletRechargePlanMapper.selectById(planId);
        if (plan == null || !Integer.valueOf(1).equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.RECHARGE_PLAN_NOT_FOUND);
        }
        return plan;
    }

    private RechargeOrderView requireView(String rechargeNo) {
        RechargeOrderView view = walletRechargeOrderMapper.selectViewByNo(rechargeNo);
        if (view == null) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_NOT_FOUND);
        }
        view.setMockPayEnabled(mockPayEnabled);
        return view;
    }

    private void applyMockFlag(List<RechargeOrderView> views) {
        for (RechargeOrderView view : views) {
            view.setMockPayEnabled(mockPayEnabled);
        }
    }

    private void validatePlan(RechargePlanRequest request) {
        if (request.getPrincipalAmount() == null || request.getPrincipalAmount() <= 0) {
            throw new BusinessException(ErrorCode.RECHARGE_PLAN_INVALID);
        }
        if (request.getBonusAmount() != null && request.getBonusAmount() < 0) {
            throw new BusinessException(ErrorCode.RECHARGE_PLAN_INVALID);
        }
        if (request.getBonusValidDays() != null && request.getBonusValidDays() < 0) {
            throw new BusinessException(ErrorCode.RECHARGE_PLAN_INVALID);
        }
    }

    private String buildPlanSnapshot(WalletRechargePlan plan) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("planName", plan.getPlanName());
        snapshot.put("principalAmount", plan.getPrincipalAmount());
        snapshot.put("bonusAmount", plan.getBonusAmount());
        snapshot.put("bonusValidDays", plan.getBonusValidDays());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.error("序列化充值方案快照失败: planId={}", plan.getId(), e);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    private int getBonusValidDays(WalletRechargeOrder order) {
        if (order.getPlanSnapshot() == null || order.getPlanSnapshot().isBlank()) {
            return 0;
        }
        try {
            JsonNode node = objectMapper.readTree(order.getPlanSnapshot());
            return node.path("bonusValidDays").asInt(0);
        } catch (Exception e) {
            log.warn("解析充值方案快照失败: rechargeNo={}", order.getRechargeNo());
            return 0;
        }
    }

    private String generateNo(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }

    private long nvl(Long value) {
        return value != null ? value : 0L;
    }

}
