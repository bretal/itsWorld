package com.itsworld.commerce.catalog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.commerce.catalog.dto.ShopCategorySaveRequest;
import com.itsworld.commerce.catalog.dto.ShopProductDetailView;
import com.itsworld.commerce.catalog.dto.ShopProductImageSaveRequest;
import com.itsworld.commerce.catalog.dto.ShopProductSaveRequest;
import com.itsworld.commerce.catalog.dto.ShopProductView;
import com.itsworld.commerce.catalog.dto.ShopSkuSaveRequest;
import com.itsworld.commerce.catalog.dto.ShopSkuView;
import com.itsworld.commerce.catalog.entity.ShopCategory;
import com.itsworld.commerce.catalog.entity.ShopProduct;
import com.itsworld.commerce.catalog.entity.ShopProductImage;
import com.itsworld.commerce.catalog.entity.ShopSku;
import com.itsworld.commerce.catalog.mapper.ShopCategoryMapper;
import com.itsworld.commerce.catalog.mapper.ShopProductImageMapper;
import com.itsworld.commerce.catalog.mapper.ShopProductMapper;
import com.itsworld.commerce.catalog.mapper.ShopSkuMapper;
import com.itsworld.commerce.common.CommercePriceUtils;
import com.itsworld.commerce.inventory.entity.ShopInventory;
import com.itsworld.commerce.inventory.mapper.ShopInventoryMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商城商品业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopCatalogService {

    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";
    private static final String ON_SALE = "ON_SALE";
    private static final String OFF_SALE = "OFF_SALE";
    private static final long DEFAULT_WAREHOUSE_ID = 0L;

    private final ShopCategoryMapper categoryMapper;
    private final ShopProductMapper productMapper;
    private final ShopProductImageMapper imageMapper;
    private final ShopSkuMapper skuMapper;
    private final ShopInventoryMapper inventoryMapper;
    private final MemberService memberService;

    public List<ShopCategory> listEnabledCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ShopCategory>()
                .eq(ShopCategory::getStatus, ENABLED)
                .orderByAsc(ShopCategory::getSort)
                .orderByAsc(ShopCategory::getId));
    }

    public PageResult<ShopProductView> pageProducts(Long categoryId, String keyword,
                                                    long pageNum, long pageSize, Long userId) {
        Page<ShopProduct> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getSaleStatus, ON_SALE)
                .orderByAsc(ShopProduct::getSort)
                .orderByAsc(ShopProduct::getId);
        if (categoryId != null) {
            wrapper.eq(ShopProduct::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(ShopProduct::getName, value)
                    .or().like(ShopProduct::getSubtitle, value)
                    .or().like(ShopProduct::getProductCode, value));
        }
        Page<ShopProduct> result = productMapper.selectPage(page, wrapper);
        BigDecimal discountRate = memberService.getCurrentDiscountRate(userId);
        Map<Long, String> categoryNames = categoryNameMap();
        List<ShopProductView> records = new ArrayList<>();
        for (ShopProduct product : result.getRecords()) {
            List<ShopSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ShopSku>()
                    .eq(ShopSku::getProductId, product.getId())
                    .eq(ShopSku::getStatus, ENABLED));
            Long minPrice = null;
            Long minMemberPrice = null;
            for (ShopSku sku : skus) {
                long price = sku.getSalePrice() != null ? sku.getSalePrice() : 0L;
                if (minPrice == null || price < minPrice) {
                    minPrice = price;
                }
                long memberPrice = CommercePriceUtils.applyDiscount(price, discountRate);
                if (minMemberPrice == null || memberPrice < minMemberPrice) {
                    minMemberPrice = memberPrice;
                }
            }
            records.add(ShopProductView.builder()
                    .id(product.getId())
                    .productCode(product.getProductCode())
                    .categoryId(product.getCategoryId())
                    .categoryName(categoryNames.get(product.getCategoryId()))
                    .name(product.getName())
                    .subtitle(product.getSubtitle())
                    .coverUrl(product.getCoverUrl())
                    .saleStatus(product.getSaleStatus())
                    .minPrice(minPrice)
                    .minMemberPrice(minMemberPrice)
                    .sort(product.getSort())
                    .build());
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ShopProductDetailView getProductDetail(Long productId, Long userId) {
        ShopProduct product = requireProduct(productId);
        if (!ON_SALE.equals(product.getSaleStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "商品已下架");
        }
        return buildProductDetail(product, memberService.getCurrentDiscountRate(userId), true);
    }

    public List<ShopCategory> listAdminCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ShopCategory>()
                .orderByAsc(ShopCategory::getSort)
                .orderByAsc(ShopCategory::getId));
    }

    @Transactional
    public ShopCategory createCategory(ShopCategorySaveRequest request) {
        validateParentCategory(request.getParentId());
        ShopCategory category = ShopCategory.builder()
                .parentId(request.getParentId())
                .name(request.getName().trim())
                .iconUrl(request.getIconUrl())
                .sort(request.getSort() != null ? request.getSort() : 0)
                .status(normalizeStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        categoryMapper.insert(category);
        return category;
    }

    @Transactional
    public ShopCategory updateCategory(Long categoryId, ShopCategorySaveRequest request) {
        ShopCategory category = requireCategory(categoryId);
        if (request.getParentId() != null && request.getParentId().equals(categoryId)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "分类不能以自身为父分类");
        }
        validateParentCategory(request.getParentId());
        category.setParentId(request.getParentId());
        category.setName(request.getName().trim());
        category.setIconUrl(request.getIconUrl());
        category.setSort(request.getSort() != null ? request.getSort() : category.getSort());
        category.setStatus(normalizeStatus(request.getStatus()));
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
        return category;
    }

    @Transactional
    public void changeCategoryStatus(Long categoryId, String status) {
        ShopCategory category = requireCategory(categoryId);
        category.setStatus(normalizeStatus(status));
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    public PageResult<ShopProduct> pageAdminProducts(Long categoryId, String keyword,
                                                     long pageNum, long pageSize) {
        Page<ShopProduct> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<ShopProduct>()
                .orderByAsc(ShopProduct::getSort)
                .orderByAsc(ShopProduct::getId);
        if (categoryId != null) {
            wrapper.eq(ShopProduct::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(ShopProduct::getName, value)
                    .or().like(ShopProduct::getProductCode, value)
                    .or().like(ShopProduct::getSubtitle, value));
        }
        Page<ShopProduct> result = productMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ShopProductDetailView getAdminProductDetail(Long productId) {
        ShopProduct product = requireProduct(productId);
        return buildProductDetail(product, BigDecimal.ONE, false);
    }

    @Transactional
    public ShopProduct createProduct(ShopProductSaveRequest request) {
        validateCategory(request.getCategoryId());
        validateProductCode(request.getProductCode(), null);
        ShopProduct product = ShopProduct.builder()
                .productCode(request.getProductCode().trim())
                .categoryId(request.getCategoryId())
                .name(request.getName().trim())
                .subtitle(request.getSubtitle())
                .description(request.getDescription())
                .coverUrl(request.getCoverUrl())
                .saleStatus(normalizeSaleStatus(request.getSaleStatus()))
                .sort(request.getSort() != null ? request.getSort() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        productMapper.insert(product);
        return product;
    }

    @Transactional
    public ShopProduct updateProduct(Long productId, ShopProductSaveRequest request) {
        ShopProduct product = requireProduct(productId);
        validateCategory(request.getCategoryId());
        validateProductCode(request.getProductCode(), productId);
        product.setProductCode(request.getProductCode().trim());
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName().trim());
        product.setSubtitle(request.getSubtitle());
        product.setDescription(request.getDescription());
        product.setCoverUrl(request.getCoverUrl());
        product.setSaleStatus(normalizeSaleStatus(request.getSaleStatus()));
        product.setSort(request.getSort() != null ? request.getSort() : product.getSort());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return product;
    }

    @Transactional
    public void changeProductSaleStatus(Long productId, String saleStatus) {
        ShopProduct product = requireProduct(productId);
        product.setSaleStatus(normalizeSaleStatus(saleStatus));
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    public List<ShopProductImage> listImages(Long productId) {
        requireProduct(productId);
        return imageMapper.selectList(new LambdaQueryWrapper<ShopProductImage>()
                .eq(ShopProductImage::getProductId, productId)
                .orderByAsc(ShopProductImage::getSort)
                .orderByAsc(ShopProductImage::getId));
    }

    @Transactional
    public ShopProductImage addImage(Long productId, ShopProductImageSaveRequest request) {
        requireProduct(productId);
        ShopProductImage image = ShopProductImage.builder()
                .productId(productId)
                .imageUrl(request.getImageUrl())
                .sort(request.getSort() != null ? request.getSort() : 0)
                .createdAt(LocalDateTime.now())
                .build();
        imageMapper.insert(image);
        return image;
    }

    @Transactional
    public void deleteImage(Long imageId) {
        imageMapper.deleteById(imageId);
    }

    @Transactional
    public ShopSku createSku(Long productId, ShopSkuSaveRequest request) {
        requireProduct(productId);
        validateSkuCode(request.getSkuCode(), null);
        ShopSku sku = ShopSku.builder()
                .productId(productId)
                .skuCode(request.getSkuCode().trim())
                .specJson(request.getSpecJson())
                .salePrice(request.getSalePrice())
                .marketPrice(request.getMarketPrice())
                .weightGram(request.getWeightGram())
                .status(normalizeStatus(request.getStatus()))
                .version(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        skuMapper.insert(sku);
        ShopInventory inventory = ShopInventory.builder()
                .skuId(sku.getId())
                .warehouseId(DEFAULT_WAREHOUSE_ID)
                .availableQty(0)
                .lockedQty(0)
                .soldQty(0)
                .version(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            inventoryMapper.insert(inventory);
        } catch (DuplicateKeyException e) {
            log.info("SKU库存已存在: skuId={}", sku.getId());
        }
        return sku;
    }

    @Transactional
    public ShopSku updateSku(Long skuId, ShopSkuSaveRequest request) {
        ShopSku sku = requireSku(skuId);
        validateSkuCode(request.getSkuCode(), skuId);
        sku.setSkuCode(request.getSkuCode().trim());
        sku.setSpecJson(request.getSpecJson());
        sku.setSalePrice(request.getSalePrice());
        sku.setMarketPrice(request.getMarketPrice());
        sku.setWeightGram(request.getWeightGram());
        sku.setStatus(normalizeStatus(request.getStatus()));
        sku.setVersion(sku.getVersion() != null ? sku.getVersion() + 1 : 1);
        sku.setUpdatedAt(LocalDateTime.now());
        skuMapper.updateById(sku);
        return sku;
    }

    @Transactional
    public void changeSkuStatus(Long skuId, String status) {
        ShopSku sku = requireSku(skuId);
        sku.setStatus(normalizeStatus(status));
        sku.setVersion(sku.getVersion() != null ? sku.getVersion() + 1 : 1);
        sku.setUpdatedAt(LocalDateTime.now());
        skuMapper.updateById(sku);
    }

    private ShopProductDetailView buildProductDetail(ShopProduct product, BigDecimal discountRate,
                                                     boolean onlyEnabledSkus) {
        List<ShopProductImage> images = imageMapper.selectList(new LambdaQueryWrapper<ShopProductImage>()
                .eq(ShopProductImage::getProductId, product.getId())
                .orderByAsc(ShopProductImage::getSort)
                .orderByAsc(ShopProductImage::getId));
        LambdaQueryWrapper<ShopSku> skuWrapper = new LambdaQueryWrapper<ShopSku>()
                .eq(ShopSku::getProductId, product.getId())
                .orderByAsc(ShopSku::getId);
        if (onlyEnabledSkus) {
            skuWrapper.eq(ShopSku::getStatus, ENABLED);
        }
        List<ShopSku> skus = skuMapper.selectList(skuWrapper);
        List<ShopSkuView> skuViews = new ArrayList<>();
        for (ShopSku sku : skus) {
            ShopInventory inventory = inventoryMapper.selectBySkuIdAndWarehouse(
                    sku.getId(), DEFAULT_WAREHOUSE_ID);
            skuViews.add(ShopSkuView.builder()
                    .id(sku.getId())
                    .productId(sku.getProductId())
                    .skuCode(sku.getSkuCode())
                    .specJson(sku.getSpecJson())
                    .salePrice(sku.getSalePrice())
                    .marketPrice(sku.getMarketPrice())
                    .memberPrice(CommercePriceUtils.applyDiscount(
                            sku.getSalePrice() != null ? sku.getSalePrice() : 0L, discountRate))
                    .weightGram(sku.getWeightGram())
                    .status(sku.getStatus())
                    .availableQty(inventory != null && inventory.getAvailableQty() != null
                            ? inventory.getAvailableQty() : 0)
                    .build());
        }
        return ShopProductDetailView.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .categoryId(product.getCategoryId())
                .categoryName(categoryNameMap().get(product.getCategoryId()))
                .name(product.getName())
                .subtitle(product.getSubtitle())
                .description(product.getDescription())
                .coverUrl(product.getCoverUrl())
                .saleStatus(product.getSaleStatus())
                .sort(product.getSort())
                .images(images)
                .skus(skuViews)
                .build();
    }

    private Map<Long, String> categoryNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (ShopCategory category : categoryMapper.selectList(null)) {
            map.put(category.getId(), category.getName());
        }
        return map;
    }

    private ShopProduct requireProduct(Long productId) {
        ShopProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "商品不存在");
        }
        return product;
    }

    private ShopSku requireSku(Long skuId) {
        ShopSku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "SKU不存在");
        }
        return sku;
    }

    private ShopCategory requireCategory(Long categoryId) {
        ShopCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "商品分类不存在");
        }
        return category;
    }

    private void validateCategory(Long categoryId) {
        if (categoryId != null) {
            requireCategory(categoryId);
        }
    }

    private void validateParentCategory(Long parentId) {
        if (parentId != null) {
            requireCategory(parentId);
        }
    }

    private void validateProductCode(String productCode, Long excludeId) {
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getProductCode, productCode.trim());
        if (excludeId != null) {
            wrapper.ne(ShopProduct::getId, excludeId);
        }
        Long count = productMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "商品编码已存在");
        }
    }

    private void validateSkuCode(String skuCode, Long excludeId) {
        LambdaQueryWrapper<ShopSku> wrapper = new LambdaQueryWrapper<ShopSku>()
                .eq(ShopSku::getSkuCode, skuCode.trim());
        if (excludeId != null) {
            wrapper.ne(ShopSku::getId, excludeId);
        }
        Long count = skuMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "SKU编码已存在");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return ENABLED;
        }
        String value = status.trim().toUpperCase();
        if (!ENABLED.equals(value) && !DISABLED.equals(value)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "状态仅支持 ENABLED/DISABLED");
        }
        return value;
    }

    private String normalizeSaleStatus(String saleStatus) {
        if (saleStatus == null || saleStatus.isBlank()) {
            return OFF_SALE;
        }
        String value = saleStatus.trim().toUpperCase();
        if (!ON_SALE.equals(value) && !OFF_SALE.equals(value)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "销售状态仅支持 ON_SALE/OFF_SALE");
        }
        return value;
    }

}
