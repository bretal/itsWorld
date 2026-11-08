package com.itsworld.commerce.order.service;

import com.itsworld.commerce.order.dto.PaymentCreateRequest;
import com.itsworld.commerce.order.dto.PaymentResultView;

/**
 * 商业支付业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface CommercePaymentService {

    PaymentResultView createPayment(Long userId, String orderNo, PaymentCreateRequest request);

    PaymentResultView getPaymentStatus(Long userId, String orderNo);

}
