package com.itsworld.commerce.order.service;

import com.itsworld.commerce.order.dto.CheckoutPreviewRequest;
import com.itsworld.commerce.order.dto.CheckoutPreviewResponse;
import com.itsworld.commerce.order.dto.CheckoutSubmitRequest;
import com.itsworld.commerce.order.dto.OrderSubmitResponse;

/**
 * 结算业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface CheckoutService {

    CheckoutPreviewResponse preview(Long userId, CheckoutPreviewRequest request);

    OrderSubmitResponse submit(Long userId, CheckoutSubmitRequest request);

}
