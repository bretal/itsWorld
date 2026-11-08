package com.itsworld.commerce.catalog.service;

import com.itsworld.commerce.catalog.dto.ShopCategorySaveRequest;
import com.itsworld.commerce.catalog.dto.ShopProductDetailView;
import com.itsworld.commerce.catalog.dto.ShopProductImageSaveRequest;
import com.itsworld.commerce.catalog.dto.ShopProductSaveRequest;
import com.itsworld.commerce.catalog.dto.ShopProductView;
import com.itsworld.commerce.catalog.dto.ShopSkuSaveRequest;
import com.itsworld.commerce.catalog.entity.ShopCategory;
import com.itsworld.commerce.catalog.entity.ShopProduct;
import com.itsworld.commerce.catalog.entity.ShopProductImage;
import com.itsworld.commerce.catalog.entity.ShopSku;
import com.itsworld.common.result.PageResult;

import java.util.List;

/**
 * 商城商品业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface ShopCatalogService {

    List<ShopCategory> listEnabledCategories();

    PageResult<ShopProductView> pageProducts(Long categoryId, String keyword,
                                             long pageNum, long pageSize, Long userId);

    ShopProductDetailView getProductDetail(Long productId, Long userId);

    List<ShopCategory> listAdminCategories();

    ShopCategory createCategory(ShopCategorySaveRequest request);

    ShopCategory updateCategory(Long categoryId, ShopCategorySaveRequest request);

    void changeCategoryStatus(Long categoryId, String status);

    PageResult<ShopProduct> pageAdminProducts(Long categoryId, String keyword,
                                              long pageNum, long pageSize);

    ShopProductDetailView getAdminProductDetail(Long productId);

    ShopProduct createProduct(ShopProductSaveRequest request);

    ShopProduct updateProduct(Long productId, ShopProductSaveRequest request);

    void changeProductSaleStatus(Long productId, String saleStatus);

    List<ShopProductImage> listImages(Long productId);

    ShopProductImage addImage(Long productId, ShopProductImageSaveRequest request);

    void deleteImage(Long imageId);

    ShopSku createSku(Long productId, ShopSkuSaveRequest request);

    ShopSku updateSku(Long skuId, ShopSkuSaveRequest request);

    void changeSkuStatus(Long skuId, String status);

}
