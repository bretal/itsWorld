package com.itsworld.commerce.grooming.controller;

import com.itsworld.commerce.grooming.dto.GroomingServiceDetailView;
import com.itsworld.commerce.grooming.dto.GroomingServiceView;
import com.itsworld.commerce.grooming.entity.GroomingCategory;
import com.itsworld.commerce.grooming.service.GroomingCatalogService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端美容服务控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/grooming")
@RequiredArgsConstructor
public class GroomingController {

    private final GroomingCatalogService groomingCatalogService;
    private final CurrentUserService currentUserService;

    @GetMapping("/categories")
    public Result<List<GroomingCategory>> listCategories() {
        return Result.success(groomingCatalogService.listEnabledCategories());
    }

    @GetMapping("/services")
    public Result<PageResult<GroomingServiceView>> listServices(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        Long userId = currentUserService.getUserId();
        return Result.success(groomingCatalogService.pageServices(
                storeId, categoryId, keyword,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size), userId));
    }

    @GetMapping("/services/{serviceId}")
    public Result<GroomingServiceDetailView> getServiceDetail(
            @PathVariable Long serviceId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long petId) {
        Long userId = currentUserService.getUserId();
        return Result.success(groomingCatalogService.getServiceDetail(serviceId, storeId, petId, userId));
    }

}
