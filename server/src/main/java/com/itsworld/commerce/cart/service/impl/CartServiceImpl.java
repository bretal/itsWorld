package com.itsworld.commerce.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itsworld.commerce.cart.dto.CartItemAddRequest;
import com.itsworld.commerce.cart.dto.CartItemUpdateRequest;
import com.itsworld.commerce.cart.dto.CartItemView;
import com.itsworld.commerce.cart.entity.ShopCartItem;
import com.itsworld.commerce.cart.mapper.ShopCartItemMapper;
import com.itsworld.commerce.cart.service.CartService;
import com.itsworld.commerce.catalog.entity.ShopProduct;
import com.itsworld.commerce.catalog.entity.ShopSku;
import com.itsworld.commerce.catalog.mapper.ShopProductMapper;
import com.itsworld.commerce.catalog.mapper.ShopSkuMapper;
import com.itsworld.commerce.common.CommercePriceUtils;
import com.itsworld.commerce.inventory.entity.ShopInventory;
import com.itsworld.commerce.inventory.mapper.ShopInventoryMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final long DEFAULT_WAREHOUSE_ID = 0L;

    private final ShopCartItemMapper cartItemMapper;
    private final ShopSkuMapper skuMapper;
    private final ShopProductMapper productMapper;
    private final ShopInventoryMapper inventoryMapper;
    private final MemberService memberService;

    @Override
    public List<CartItemView> listCart(Long userId) {
        List<ShopCartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<ShopCartItem>()
                .eq(ShopCartItem::getUserId, userId)
                .orderByDesc(ShopCartItem::getId));
        BigDecimal rate = memberService.getCurrentDiscountRate(userId);
        List<CartItemView> views = new ArrayList<>();
        for (ShopCartItem item : items) {
            ShopSku sku = skuMapper.selectById(item.getSkuId());
            ShopProduct product = sku != null ? productMapper.selectById(sku.getProductId()) : null;
            ShopInventory inventory = inventoryMapper.selectBySkuIdAndWarehouse(
                    item.getSkuId(), DEFAULT_WAREHOUSE_ID);
            int available = inventory != null && inventory.getAvailableQty() != null
                    ? inventory.getAvailableQty() : 0;
            String invalidReason = resolveInvalidReason(sku, product, item.getQuantity(), available, rate);
            views.add(CartItemView.builder()
                    .id(item.getId())
                    .skuId(item.getSkuId())
                    .productId(sku != null ? sku.getProductId() : null)
                    .productName(product != null ? product.getName() : "商品已失效")
                    .productCoverUrl(product != null ? product.getCoverUrl() : null)
                    .skuCode(sku != null ? sku.getSkuCode() : null)
                    .specJson(sku != null ? sku.getSpecJson() : null)
                    .salePrice(sku != null ? sku.getSalePrice() : 0L)
                    .memberPrice(sku != null
                            ? CommercePriceUtils.applyDiscount(
                                    sku.getSalePrice() != null ? sku.getSalePrice() : 0L, rate)
                            : 0L)
                    .quantity(item.getQuantity())
                    .selected(item.getSelected())
                    .availableQty(available)
                    .invalidReason(invalidReason)
                    .build());
        }
        return views;
    }

    @Override
    @Transactional
    public void addItem(Long userId, CartItemAddRequest request) {
        ShopSku sku = skuMapper.selectById(request.getSkuId());
        if (sku == null || !"ENABLED".equals(sku.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "SKU不存在或已下架");
        }
        ShopProduct product = productMapper.selectById(sku.getProductId());
        if (product == null || !"ON_SALE".equals(product.getSaleStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "商品已下架");
        }
        ShopInventory inventory = inventoryMapper.selectBySkuIdAndWarehouse(
                sku.getId(), DEFAULT_WAREHOUSE_ID);
        int available = inventory != null && inventory.getAvailableQty() != null
                ? inventory.getAvailableQty() : 0;
        ShopCartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<ShopCartItem>()
                .eq(ShopCartItem::getUserId, userId)
                .eq(ShopCartItem::getSkuId, request.getSkuId())
                .last("LIMIT 1"));
        int newQuantity = (existing != null ? existing.getQuantity() : 0) + request.getQuantity();
        if (newQuantity > available) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(),
                    "库存不足，当前可售 " + available + " 件");
        }
        if (existing != null) {
            existing.setQuantity(newQuantity);
            existing.setSelected(1);
            existing.setInvalidReason(null);
            existing.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.updateById(existing);
        } else {
            cartItemMapper.insert(ShopCartItem.builder()
                    .userId(userId)
                    .skuId(request.getSkuId())
                    .quantity(request.getQuantity())
                    .selected(1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        }
    }

    @Override
    @Transactional
    public void updateItem(Long userId, Long cartItemId, CartItemUpdateRequest request) {
        ShopCartItem item = requireOwnedItem(userId, cartItemId);
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getSelected() != null) {
            item.setSelected(request.getSelected());
        }
        item.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        requireOwnedItem(userId, cartItemId);
        cartItemMapper.deleteById(cartItemId);
    }

    @Override
    public List<ShopCartItem> listSelectedItems(Long userId) {
        return cartItemMapper.selectList(new LambdaQueryWrapper<ShopCartItem>()
                .eq(ShopCartItem::getUserId, userId)
                .eq(ShopCartItem::getSelected, 1)
                .orderByAsc(ShopCartItem::getId));
    }

    @Override
    @Transactional
    public void deleteSelectedItems(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<ShopCartItem>()
                .eq(ShopCartItem::getUserId, userId)
                .eq(ShopCartItem::getSelected, 1));
    }

    private ShopCartItem requireOwnedItem(Long userId, Long cartItemId) {
        ShopCartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "购物车项不存在");
        }
        return item;
    }

    private String resolveInvalidReason(ShopSku sku, ShopProduct product, int quantity,
                                        int available, BigDecimal rate) {
        if (sku == null || !"ENABLED".equals(sku.getStatus())) {
            return "SKU已下架";
        }
        if (product == null || !"ON_SALE".equals(product.getSaleStatus())) {
            return "商品已下架";
        }
        if (available < quantity) {
            return "库存不足，剩余 " + available + " 件";
        }
        return null;
    }

}
