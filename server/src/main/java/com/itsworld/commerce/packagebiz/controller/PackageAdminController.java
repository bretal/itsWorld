package com.itsworld.commerce.packagebiz.controller;

import com.itsworld.commerce.packagebiz.dto.PackageTemplateSaveRequest;
import com.itsworld.commerce.packagebiz.dto.PackageTemplateView;
import com.itsworld.commerce.packagebiz.service.PackageService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端服务套餐控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin/commerce/packages")
@RequiredArgsConstructor
public class PackageAdminController {

    private final PackageService packageService;

    @GetMapping
    public Result<PageResult<PackageTemplateView>> pagePackages(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(packageService.pageAdminTemplates(status, keyword,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/{templateId}")
    public Result<PackageTemplateView> getPackage(@PathVariable Long templateId) {
        return Result.success(packageService.getAdminTemplate(templateId));
    }

    @PostMapping
    public Result<PackageTemplateView> createPackage(
            @Valid @RequestBody PackageTemplateSaveRequest request) {
        return Result.success(packageService.createTemplate(request), "套餐已创建");
    }

    @PutMapping("/{templateId}")
    public Result<PackageTemplateView> updatePackage(
            @PathVariable Long templateId,
            @Valid @RequestBody PackageTemplateSaveRequest request) {
        return Result.success(packageService.updateTemplate(templateId, request), "套餐已更新");
    }

    @PutMapping("/{templateId}/status")
    public Result<Void> changeStatus(@PathVariable Long templateId,
                                     @RequestParam String status) {
        packageService.changeTemplateStatus(templateId, status);
        return Result.success(null, "套餐状态已更新");
    }

}
