package com.itsworld.commerce.order.service;

import com.itsworld.commerce.order.dto.PaymentCreateRequest;
import com.itsworld.commerce.order.dto.PaymentResultView;
import com.itsworld.commerce.order.entity.CommerceOrder;
import com.itsworld.commerce.order.entity.CommercePaymentLink;
import com.itsworld.commerce.order.mapper.CommerceOrderMapper;
import com.itsworld.commerce.order.mapper.CommercePaymentLinkMapper;
import com.itsworld.commerce.order.service.CommerceOrderService;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.wallet.dto.WalletPaymentResult;
import com.itsworld.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商业支付业务实现
 * 当前支持 WALLET 与开发环境 MOCK_WECHAT；真实 WECHAT 待商户资质/openid。
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommercePaymentService {

    private final CommerceOrderMapper orderMapper;
    private final CommercePaymentLinkMapper paymentLinkMapper;
    private final CommerceOrderService orderService;
    private final WalletService walletService;

    @Value("${app.payment.mock-enabled:false}")
    private boolean mockPayEnabled;

    @Transactional
    public PaymentResultView createPayment(Long userId, String orderNo, PaymentCreateRequest request) {
        CommerceOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        String channel = request.getChannel() != null
                ? request.getChannel().trim().toUpperCase() : "";
        if (!"WALLET".equals(channel) && !"MOCK_WECHAT".equals(channel) && !"WECHAT".equals(channel)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "不支持的支付渠道");
        }
        if ("WECHAT".equals(channel)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(),
                    "微信支付待接入商户资质与 openid，当前请使用钱包支付或开发环境模拟支付");
        }
        if ("MOCK_WECHAT".equals(channel) && !mockPayEnabled) {
            throw new BusinessException(ErrorCode.MOCK_PAY_DISABLED);
        }
        if ("PAID".equals(order.getOrderStatus())
                || ("MEMBERSHIP".equals(order.getOrderType()) && "COMPLETED".equals(order.getOrderStatus()))) {
            CommercePaymentLink latest = paymentLinkMapper.selectLatestByOrderId(order.getId());
            if (latest != null) {
                return toView(order, latest);
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "订单已支付");
        }
        if (!"WAIT_PAY".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "当前订单状态不可支付");
        }
        if (order.getExpireTime() != null && !order.getExpireTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "订单已超时，请重新下单");
        }
        if (order.getPayableAmount() == null || order.getPayableAmount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "订单应付金额必须大于0");
        }

        String idempotencyKey = "COMMERCE_PAY:" + orderNo + ":" + channel;
        CommercePaymentLink existing = paymentLinkMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            if ("PAID".equals(existing.getStatus())) {
                return toView(order, existing);
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "支付处理中，请稍后查询");
        }

        String paymentOrderId = "PAY" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 24).toUpperCase();
        CommercePaymentLink link = CommercePaymentLink.builder()
                .orderId(order.getId())
                .paymentOrderId(paymentOrderId)
                .payChannel(channel)
                .payAmount(order.getPayableAmount())
                .status("CREATED")
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        paymentLinkMapper.insert(link);

        LocalDateTime now = LocalDateTime.now();
        if ("WALLET".equals(channel)) {
            WalletPaymentResult result = walletService.payForBusinessOrder(
                    userId, link.getId(), orderNo, order.getPayableAmount(),
                    "COMMERCE_PAY:" + orderNo);
            link.setStatus("PAID");
            link.setExternalTxNo(result.getTxNo());
            link.setPaidTime(now);
            link.setUpdatedAt(now);
            paymentLinkMapper.updateById(link);
        } else {
            link.setStatus("PAID");
            link.setExternalTxNo("MOCK-" + paymentOrderId);
            link.setPaidTime(now);
            link.setUpdatedAt(now);
            paymentLinkMapper.updateById(link);
        }

        orderService.markPaid(order.getId(), "USER", userId);
        CommerceOrder latestOrder = orderMapper.selectById(order.getId());
        log.info("订单支付完成: orderNo={}, channel={}", orderNo, channel);
        return toView(latestOrder, link);
    }

    public PaymentResultView getPaymentStatus(Long userId, String orderNo) {
        CommerceOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        CommercePaymentLink link = paymentLinkMapper.selectLatestByOrderId(order.getId());
        return toView(order, link);
    }

    private PaymentResultView toView(CommerceOrder order, CommercePaymentLink link) {
        return PaymentResultView.builder()
                .orderNo(order.getOrderNo())
                .paymentOrderId(link != null ? link.getPaymentOrderId() : null)
                .payChannel(link != null ? link.getPayChannel() : null)
                .payAmount(link != null ? link.getPayAmount() : order.getPayableAmount())
                .status(link != null ? link.getStatus() : "NONE")
                .externalTxNo(link != null ? link.getExternalTxNo() : null)
                .paidTime(link != null ? link.getPaidTime() : null)
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .build();
    }

}
