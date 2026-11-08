package com.itsworld.commerce.address.service;

import com.itsworld.commerce.address.dto.AddressSaveRequest;
import com.itsworld.commerce.address.entity.UserShippingAddress;

import java.util.List;

/**
 * 收货地址业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface AddressService {

    List<UserShippingAddress> listAddresses(Long userId);

    UserShippingAddress createAddress(Long userId, AddressSaveRequest request);

    UserShippingAddress updateAddress(Long userId, Long addressId, AddressSaveRequest request);

    void deleteAddress(Long userId, Long addressId);

    void setDefault(Long userId, Long addressId);

    UserShippingAddress getOwnedAddress(Long userId, Long addressId);

}
