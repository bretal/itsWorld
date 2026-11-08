package com.itsworld.commerce.cart.controller;

import com.itsworld.commerce.cart.dto.CartItemAddRequest;
import com.itsworld.commerce.cart.dto.CartItemUpdateRequest;
import com.itsworld.commerce.cart.dto.CartItemView;
import com.itsworld.commerce.cart.service.CartService;
import com.itsworld.common.result.Result;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<List<CartItemView>> getCart() {
        Long userId = currentUserService.getUserId();
        return Result.success(cartService.listCart(userId));
    }

    @PostMapping("/items")
    public Result<Void> addItem(@Valid @RequestBody CartItemAddRequest request) {
        Long userId = currentUserService.getUserId();
        cartService.addItem(userId, request);
        return Result.success(null, "已加入购物车");
    }

    @PutMapping("/items/{cartItemId}")
    public Result<Void> updateItem(@PathVariable Long cartItemId,
                                   @Valid @RequestBody CartItemUpdateRequest request) {
        Long userId = currentUserService.getUserId();
        cartService.updateItem(userId, cartItemId, request);
        return Result.success(null, "购物车已更新");
    }

    @DeleteMapping("/items/{cartItemId}")
    public Result<Void> removeItem(@PathVariable Long cartItemId) {
        Long userId = currentUserService.getUserId();
        cartService.removeItem(userId, cartItemId);
        return Result.success(null, "已移除");
    }

}
