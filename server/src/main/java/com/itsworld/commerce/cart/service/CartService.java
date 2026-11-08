package com.itsworld.commerce.cart.service;

import com.itsworld.commerce.cart.dto.CartItemAddRequest;
import com.itsworld.commerce.cart.dto.CartItemUpdateRequest;
import com.itsworld.commerce.cart.dto.CartItemView;
import com.itsworld.commerce.cart.entity.ShopCartItem;

import java.util.List;

/**
 * 购物车业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface CartService {

    List<CartItemView> listCart(Long userId);

    void addItem(Long userId, CartItemAddRequest request);

    void updateItem(Long userId, Long cartItemId, CartItemUpdateRequest request);

    void removeItem(Long userId, Long cartItemId);

    List<ShopCartItem> listSelectedItems(Long userId);

    void deleteSelectedItems(Long userId);

}
