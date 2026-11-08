package com.itsworld.commerce.fulfillment.controller;

import com.itsworld.commerce.fulfillment.dto.ShipmentCreateRequest;
import com.itsworld.commerce.fulfillment.dto.ShipmentView;
import com.itsworld.commerce.fulfillment.service.ShipmentService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端物流控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin/commerce/shipments")
@RequiredArgsConstructor
public class ShipmentAdminController {

    private final ShipmentService shipmentService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<ShipmentView>> pageShipments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(shipmentService.pageAdminShipments(status, keyword,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/order/{orderNo}")
    public Result<ShipmentView> getShipment(@PathVariable String orderNo) {
        return Result.success(shipmentService.getAdminShipment(orderNo));
    }

    @PostMapping("/order/{orderNo}")
    public Result<ShipmentView> shipOrder(@PathVariable String orderNo,
                                          @Valid @RequestBody ShipmentCreateRequest request) {
        return Result.success(shipmentService.shipOrder(
                orderNo, request, currentUserService.getUserId()), "订单已发货");
    }

}
