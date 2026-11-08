package com.itsworld.commerce.address.controller;

import com.itsworld.commerce.address.dto.AddressSaveRequest;
import com.itsworld.commerce.address.entity.UserShippingAddress;
import com.itsworld.commerce.address.service.AddressService;
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
 * 用户收货地址控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<List<UserShippingAddress>> listAddresses() {
        Long userId = currentUserService.getUserId();
        return Result.success(addressService.listAddresses(userId));
    }

    @PostMapping
    public Result<UserShippingAddress> createAddress(@Valid @RequestBody AddressSaveRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(addressService.createAddress(userId, request), "地址已创建");
    }

    @PutMapping("/{addressId}")
    public Result<UserShippingAddress> updateAddress(@PathVariable Long addressId,
                                                     @Valid @RequestBody AddressSaveRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(addressService.updateAddress(userId, addressId, request), "地址已更新");
    }

    @DeleteMapping("/{addressId}")
    public Result<Void> deleteAddress(@PathVariable Long addressId) {
        Long userId = currentUserService.getUserId();
        addressService.deleteAddress(userId, addressId);
        return Result.success(null, "地址已删除");
    }

    @PutMapping("/{addressId}/default")
    public Result<Void> setDefault(@PathVariable Long addressId) {
        Long userId = currentUserService.getUserId();
        addressService.setDefault(userId, addressId);
        return Result.success(null, "默认地址已设置");
    }

}
