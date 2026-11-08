package com.itsworld.commerce.catalog.controller;

import com.itsworld.commerce.catalog.dto.ShopProductDetailView;
import com.itsworld.commerce.catalog.dto.ShopProductView;
import com.itsworld.commerce.catalog.entity.ShopCategory;
import com.itsworld.commerce.catalog.service.ShopCatalogService;
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
 * 用户端商城控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopCatalogService shopCatalogService;
    private final CurrentUserService currentUserService;

    @GetMapping("/categories")
    public Result<List<ShopCategory>> listCategories() {
        return Result.success(shopCatalogService.listEnabledCategories());
    }

    @GetMapping("/products")
    public Result<PageResult<ShopProductView>> listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        Long userId = currentUserService.getUserId();
        return Result.success(shopCatalogService.pageProducts(
                categoryId, keyword,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size), userId));
    }

    @GetMapping("/products/{productId}")
    public Result<ShopProductDetailView> getProductDetail(@PathVariable Long productId) {
        Long userId = currentUserService.getUserId();
        return Result.success(shopCatalogService.getProductDetail(productId, userId));
    }

}
