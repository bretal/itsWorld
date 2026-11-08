package com.itsworld.commerce.grooming.controller;

import com.itsworld.commerce.grooming.dto.GroomingCategorySaveRequest;
import com.itsworld.commerce.grooming.dto.GroomingPetRuleSaveRequest;
import com.itsworld.commerce.grooming.dto.GroomingServiceSaveRequest;
import com.itsworld.commerce.grooming.dto.GroomingServiceStoreSaveRequest;
import com.itsworld.commerce.grooming.entity.GroomingCategory;
import com.itsworld.commerce.grooming.entity.GroomingService;
import com.itsworld.commerce.grooming.entity.GroomingServicePetRule;
import com.itsworld.commerce.grooming.entity.GroomingServiceStore;
import com.itsworld.commerce.grooming.service.GroomingCatalogService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端美容服务控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin/commerce/grooming")
@RequiredArgsConstructor
public class GroomingAdminController {

    private final GroomingCatalogService groomingCatalogService;

    @GetMapping("/categories")
    public Result<List<GroomingCategory>> listCategories() {
        return Result.success(groomingCatalogService.listAdminCategories());
    }

    @PostMapping("/categories")
    public Result<GroomingCategory> createCategory(@Valid @RequestBody GroomingCategorySaveRequest request) {
        return Result.success(groomingCatalogService.createCategory(request), "分类已创建");
    }

    @PutMapping("/categories/{categoryId}")
    public Result<GroomingCategory> updateCategory(@PathVariable Long categoryId,
                                                   @Valid @RequestBody GroomingCategorySaveRequest request) {
        return Result.success(groomingCatalogService.updateCategory(categoryId, request), "分类已更新");
    }

    @PutMapping("/categories/{categoryId}/status")
    public Result<Void> changeCategoryStatus(@PathVariable Long categoryId,
                                             @RequestParam String status) {
        groomingCatalogService.changeCategoryStatus(categoryId, status);
        return Result.success(null, "分类状态已更新");
    }

    @GetMapping("/services")
    public Result<PageResult<GroomingService>> pageServices(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(groomingCatalogService.pageAdminServices(
                categoryId, keyword,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/services/{serviceId}")
    public Result<GroomingService> getService(@PathVariable Long serviceId) {
        return Result.success(groomingCatalogService.getAdminService(serviceId));
    }

    @PostMapping("/services")
    public Result<GroomingService> createService(@Valid @RequestBody GroomingServiceSaveRequest request) {
        return Result.success(groomingCatalogService.createService(request), "服务已创建");
    }

    @PutMapping("/services/{serviceId}")
    public Result<GroomingService> updateService(@PathVariable Long serviceId,
                                                 @Valid @RequestBody GroomingServiceSaveRequest request) {
        return Result.success(groomingCatalogService.updateService(serviceId, request), "服务已更新");
    }

    @PutMapping("/services/{serviceId}/status")
    public Result<Void> changeServiceStatus(@PathVariable Long serviceId,
                                            @RequestParam String status) {
        groomingCatalogService.changeServiceStatus(serviceId, status);
        return Result.success(null, "服务状态已更新");
    }

    @GetMapping("/services/{serviceId}/stores")
    public Result<List<GroomingServiceStore>> listServiceStores(@PathVariable Long serviceId) {
        return Result.success(groomingCatalogService.listServiceStores(serviceId));
    }

    @PostMapping("/services/{serviceId}/stores")
    public Result<GroomingServiceStore> createServiceStore(
            @PathVariable Long serviceId,
            @Valid @RequestBody GroomingServiceStoreSaveRequest request) {
        return Result.success(groomingCatalogService.createServiceStore(serviceId, request), "门店价格已创建");
    }

    @PutMapping("/service-stores/{id}")
    public Result<GroomingServiceStore> updateServiceStore(
            @PathVariable Long id,
            @Valid @RequestBody GroomingServiceStoreSaveRequest request) {
        return Result.success(groomingCatalogService.updateServiceStore(id, request), "门店价格已更新");
    }

    @DeleteMapping("/service-stores/{id}")
    public Result<Void> deleteServiceStore(@PathVariable Long id) {
        groomingCatalogService.deleteServiceStore(id);
        return Result.success(null, "门店价格已删除");
    }

    @GetMapping("/services/{serviceId}/pet-rules")
    public Result<List<GroomingServicePetRule>> listPetRules(@PathVariable Long serviceId) {
        return Result.success(groomingCatalogService.listPetRules(serviceId));
    }

    @PostMapping("/services/{serviceId}/pet-rules")
    public Result<GroomingServicePetRule> createPetRule(
            @PathVariable Long serviceId,
            @Valid @RequestBody GroomingPetRuleSaveRequest request) {
        return Result.success(groomingCatalogService.createPetRule(serviceId, request), "宠物适配规则已创建");
    }

    @PutMapping("/pet-rules/{ruleId}")
    public Result<GroomingServicePetRule> updatePetRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody GroomingPetRuleSaveRequest request) {
        return Result.success(groomingCatalogService.updatePetRule(ruleId, request), "宠物适配规则已更新");
    }

    @DeleteMapping("/pet-rules/{ruleId}")
    public Result<Void> deletePetRule(@PathVariable Long ruleId) {
        groomingCatalogService.deletePetRule(ruleId);
        return Result.success(null, "宠物适配规则已删除");
    }

}
