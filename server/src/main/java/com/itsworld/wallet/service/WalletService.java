package com.itsworld.wallet.service;

import com.itsworld.common.result.PageResult;
import com.itsworld.wallet.dto.AdjustBalanceRequest;
import com.itsworld.wallet.dto.OfflineRechargeCreateRequest;
import com.itsworld.wallet.dto.RechargeOrderView;
import com.itsworld.wallet.dto.RechargePlanRequest;
import com.itsworld.wallet.dto.WalletAdjustmentView;
import com.itsworld.wallet.dto.WalletPaymentResult;
import com.itsworld.wallet.dto.WalletRefundResult;
import com.itsworld.wallet.dto.WalletSummaryResponse;
import com.itsworld.wallet.dto.WalletTransactionView;
import com.itsworld.wallet.entity.WalletRechargePlan;

import java.util.List;

/**
 * 钱包业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface WalletService {

    /**
     * 钱包余额摘要
     *
     * @param userId 用户ID
     * @return 余额摘要
     */
    WalletSummaryResponse getSummary(Long userId);

    /**
     * 用户侧启用中的充值方案
     *
     * @return 方案列表
     */
    List<WalletRechargePlan> getActivePlans();

    /**
     * 用户充值单分页
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<RechargeOrderView> getUserRecharges(Long userId, long pageNum, long pageSize);

    /**
     * 创建线上充值单
     *
     * @param userId 用户ID
     * @param planId 方案ID
     * @return 充值单
     */
    RechargeOrderView createRecharge(Long userId, Long planId);

    /**
     * 查询本人充值单
     *
     * @param userId     用户ID
     * @param rechargeNo 充值单号
     * @return 充值单
     */
    RechargeOrderView getRecharge(Long userId, String rechargeNo);

    /**
     * 开发环境模拟支付并到账
     *
     * @param userId     用户ID
     * @param rechargeNo 充值单号
     * @return 充值单
     */
    RechargeOrderView mockPay(Long userId, String rechargeNo);

    /**
     * 用户资金流水分页
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<WalletTransactionView> getUserTransactions(Long userId, long pageNum, long pageSize);

    /**
     * 使用钱包支付业务订单（Phase 2 新方法）
     *
     * @param userId         用户ID
     * @param paymentLinkId  商业支付记录ID（commerce_payment_link.id）
     * @param orderNo        业务订单号
     * @param amount         支付金额（分）
     * @param idempotencyKey 幂等键
     * @return 支付结果
     */
    WalletPaymentResult payForBusinessOrder(Long userId, Long paymentLinkId, String orderNo,
                                            long amount, String idempotencyKey);

    /**
     * 商业订单钱包退款：按原支付分摊原路退回原批次
     *
     * @param userId         用户ID
     * @param paymentLinkId  商业支付记录ID（commerce_payment_link.id）
     * @param orderNo        业务订单号
     * @param amount         退款金额（分）
     * @param reason         退款原因
     * @param refundNo       退款单号（幂等键）
     * @return 退款结果
     */
    WalletRefundResult refundBusinessPayment(Long userId, Long paymentLinkId, String orderNo,
                                             long amount, String reason, String refundNo);

    /**
     * 管理端全部充值方案
     *
     * @return 方案列表
     */
    List<WalletRechargePlan> getAdminPlans();

    /**
     * 新建充值方案
     *
     * @param request 方案配置
     * @return 方案
     */
    WalletRechargePlan createPlan(RechargePlanRequest request);

    /**
     * 更新充值方案
     *
     * @param planId  方案ID
     * @param request 方案配置
     * @return 方案
     */
    WalletRechargePlan updatePlan(Long planId, RechargePlanRequest request);

    /**
     * 下架充值方案
     *
     * @param planId 方案ID
     */
    void disablePlan(Long planId);

    /**
     * 管理端线下充值分页
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param paymentStatus 支付状态过滤，可为空
     * @return 分页结果
     */
    PageResult<RechargeOrderView> getOfflineRecharges(long pageNum, long pageSize, String paymentStatus);

    /**
     * 创建线下充值申请
     *
     * @param operatorId 操作人ID
     * @param request    申请信息
     * @return 充值单
     */
    RechargeOrderView createOfflineRecharge(Long operatorId, OfflineRechargeCreateRequest request);

    /**
     * 审批线下充值（确认收款并到账）
     *
     * @param approverId  审批人ID
     * @param rechargeNo  充值单号
     * @param externalTxNo 线下凭证号
     * @return 充值单
     */
    RechargeOrderView approveOfflineRecharge(Long approverId, String rechargeNo, String externalTxNo);

    /**
     * 驳回线下充值
     *
     * @param approverId  审批人ID
     * @param rechargeNo  充值单号
     * @param rejectReason 驳回原因
     * @return 充值单
     */
    RechargeOrderView rejectOfflineRecharge(Long approverId, String rechargeNo, String rejectReason);

    /**
     * 管理端全部资金流水
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param txType   交易类型过滤，可为空
     * @return 分页结果
     */
    PageResult<WalletTransactionView> getAllTransactions(long pageNum, long pageSize, String txType);

    /**
     * 管理员调增/调减用户余额
     *
     * @param operatorId 操作人ID
     * @param request    调账请求
     * @return 调账记录
     */
    WalletAdjustmentView adjustBalance(Long operatorId, AdjustBalanceRequest request);

    /**
     * 管理端调账记录分页
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param userId   用户ID过滤，可为空
     * @return 分页结果
     */
    PageResult<WalletAdjustmentView> getAdjustments(long pageNum, long pageSize, Long userId);

}
