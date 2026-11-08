package com.itsworld.commerce.packagebiz.controller;

import com.itsworld.commerce.packagebiz.dto.PackagePurchasePreview;
import com.itsworld.commerce.packagebiz.dto.PackagePurchasePreviewRequest;
import com.itsworld.commerce.packagebiz.dto.PackagePurchaseRequest;
import com.itsworld.commerce.packagebiz.dto.PackagePurchaseResult;
import com.itsworld.commerce.packagebiz.dto.PackageTemplateView;
import com.itsworld.commerce.packagebiz.dto.PackageUsageView;
import com.itsworld.commerce.packagebiz.dto.UserPackageView;
import com.itsworld.commerce.packagebiz.service.PackageService;
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

import java.util.List;

/**
 * 用户端服务套餐控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;
    private final CurrentUserService currentUserService;

    @GetMapping("/packages")
    public Result<PageResult<PackageTemplateView>> listPackages(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(packageService.pageTemplates(keyword,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/packages/{templateId}")
    public Result<PackageTemplateView> getPackage(@PathVariable Long templateId) {
        return Result.success(packageService.getTemplateDetail(templateId));
    }

    @PostMapping("/packages/preview")
    public Result<PackagePurchasePreview> preview(
            @Valid @RequestBody PackagePurchasePreviewRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(packageService.previewPurchase(userId, request));
    }

    @PostMapping("/packages/purchase")
    public Result<PackagePurchaseResult> purchase(
            @Valid @RequestBody PackagePurchaseRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(packageService.purchasePackage(userId, request), "套餐订单已创建");
    }

    @GetMapping("/my-packages")
    public Result<PageResult<UserPackageView>> listMyPackages(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        Long userId = currentUserService.getUserId();
        return Result.success(packageService.pageMyPackages(userId, status,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/my-packages/{userPackageId}")
    public Result<UserPackageView> getMyPackage(@PathVariable Long userPackageId) {
        Long userId = currentUserService.getUserId();
        return Result.success(packageService.getMyPackage(userId, userPackageId));
    }

    @GetMapping("/my-packages/{userPackageId}/usages")
    public Result<List<PackageUsageView>> listUsages(@PathVariable Long userPackageId) {
        Long userId = currentUserService.getUserId();
        return Result.success(packageService.listUsages(userId, userPackageId));
    }

}
