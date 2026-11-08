package com.itsworld.commerce.inventory.controller;

import com.itsworld.commerce.inventory.dto.InventoryAdjustRequest;
import com.itsworld.commerce.inventory.dto.InventoryLogView;
import com.itsworld.commerce.inventory.dto.InventoryView;
import com.itsworld.commerce.inventory.service.InventoryService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端库存控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin/commerce/inventory")
@RequiredArgsConstructor
public class InventoryAdminController {

    private final InventoryService inventoryService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<InventoryView>> pageInventory(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String keyword) {
        return Result.success(inventoryService.pageInventory(
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size), keyword));
    }

    @PostMapping("/adjust")
    public Result<InventoryView> adjustStock(@Valid @RequestBody InventoryAdjustRequest request) {
        Long operatorId = currentUserService.getUserId();
        return Result.success(inventoryService.adjustStock(operatorId, request), "库存已调整");
    }

    @GetMapping("/logs")
    public Result<PageResult<InventoryLogView>> pageLogs(
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(inventoryService.pageLogs(
                skuId, PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

}
