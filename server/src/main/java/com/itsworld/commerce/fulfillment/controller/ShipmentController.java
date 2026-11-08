package com.itsworld.commerce.fulfillment.controller;

import com.itsworld.commerce.fulfillment.dto.ShipmentView;
import com.itsworld.commerce.fulfillment.service.ShipmentService;
import com.itsworld.common.result.Result;
import com.itsworld.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端物流控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/orders")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{orderNo}/shipment")
    public Result<ShipmentView> getShipment(@PathVariable String orderNo) {
        Long userId = currentUserService.getUserId();
        return Result.success(shipmentService.getUserShipment(userId, orderNo));
    }

    @PostMapping("/{orderNo}/confirm-receipt")
    public Result<Void> confirmReceipt(@PathVariable String orderNo) {
        Long userId = currentUserService.getUserId();
        shipmentService.confirmReceipt(userId, orderNo);
        return Result.success(null, "已确认收货");
    }

}
