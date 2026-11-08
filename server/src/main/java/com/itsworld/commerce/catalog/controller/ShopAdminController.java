package com.itsworld.commerce.catalog.controller;

import com.itsworld.commerce.catalog.dto.ShopCategorySaveRequest;
import com.itsworld.commerce.catalog.dto.ShopProductDetailView;
import com.itsworld.commerce.catalog.dto.ShopProductImageSaveRequest;
import com.itsworld.commerce.catalog.dto.ShopProductSaveRequest;
import com.itsworld.commerce.catalog.entity.ShopCategory;
import com.itsworld.commerce.catalog.entity.ShopProduct;
import com.itsworld.commerce.catalog.entity.ShopProductImage;
import com.itsworld.commerce.catalog.entity.ShopSku;
import com.itsworld.commerce.catalog.dto.ShopSkuSaveRequest;
import com.itsworld.commerce.catalog.service.ShopCatalogService;
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
 * 管理端商城控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin/commerce/shop")
@RequiredArgsConstructor
public class ShopAdminController {

    private final ShopCatalogService shopCatalogService;

    @GetMapping("/categories")
    public Result<List<ShopCategory>> listCategories() {
        return Result.success(shopCatalogService.listAdminCategories());
    }

    @PostMapping("/categories")
    public Result<ShopCategory> createCategory(@Valid @RequestBody ShopCategorySaveRequest request) {
        return Result.success(shopCatalogService.createCategory(request), "分类已创建");
    }

    @PutMapping("/categories/{categoryId}")
    public Result<ShopCategory> updateCategory(@PathVariable Long categoryId,
                                               @Valid @RequestBody ShopCategorySaveRequest request) {
        return Result.success(shopCatalogService.updateCategory(categoryId, request), "分类已更新");
    }

    @PutMapping("/categories/{categoryId}/status")
    public Result<Void> changeCategoryStatus(@PathVariable Long categoryId,
                                             @RequestParam String status) {
        shopCatalogService.changeCategoryStatus(categoryId, status);
        return Result.success(null, "分类状态已更新");
    }

    @GetMapping("/products")
    public Result<PageResult<ShopProduct>> pageProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(shopCatalogService.pageAdminProducts(
                categoryId, keyword,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/products/{productId}")
    public Result<ShopProductDetailView> getProductDetail(@PathVariable Long productId) {
        return Result.success(shopCatalogService.getAdminProductDetail(productId));
    }

    @PostMapping("/products")
    public Result<ShopProduct> createProduct(@Valid @RequestBody ShopProductSaveRequest request) {
        return Result.success(shopCatalogService.createProduct(request), "商品已创建");
    }

    @PutMapping("/products/{productId}")
    public Result<ShopProduct> updateProduct(@PathVariable Long productId,
                                             @Valid @RequestBody ShopProductSaveRequest request) {
        return Result.success(shopCatalogService.updateProduct(productId, request), "商品已更新");
    }

    @PutMapping("/products/{productId}/sale-status")
    public Result<Void> changeSaleStatus(@PathVariable Long productId,
                                         @RequestParam String saleStatus) {
        shopCatalogService.changeProductSaleStatus(productId, saleStatus);
        return Result.success(null, "商品销售状态已更新");
    }

    @GetMapping("/products/{productId}/images")
    public Result<List<ShopProductImage>> listImages(@PathVariable Long productId) {
        return Result.success(shopCatalogService.listImages(productId));
    }

    @PostMapping("/products/{productId}/images")
    public Result<ShopProductImage> addImage(
            @PathVariable Long productId,
            @Valid @RequestBody ShopProductImageSaveRequest request) {
        return Result.success(shopCatalogService.addImage(productId, request), "图片已添加");
    }

    @DeleteMapping("/images/{imageId}")
    public Result<Void> deleteImage(@PathVariable Long imageId) {
        shopCatalogService.deleteImage(imageId);
        return Result.success(null, "图片已删除");
    }

    @PostMapping("/products/{productId}/skus")
    public Result<ShopSku> createSku(@PathVariable Long productId,
                                     @Valid @RequestBody ShopSkuSaveRequest request) {
        return Result.success(shopCatalogService.createSku(productId, request), "SKU已创建");
    }

    @PutMapping("/skus/{skuId}")
    public Result<ShopSku> updateSku(@PathVariable Long skuId,
                                     @Valid @RequestBody ShopSkuSaveRequest request) {
        return Result.success(shopCatalogService.updateSku(skuId, request), "SKU已更新");
    }

    @PutMapping("/skus/{skuId}/status")
    public Result<Void> changeSkuStatus(@PathVariable Long skuId,
                                        @RequestParam String status) {
        shopCatalogService.changeSkuStatus(skuId, status);
        return Result.success(null, "SKU状态已更新");
    }

}
