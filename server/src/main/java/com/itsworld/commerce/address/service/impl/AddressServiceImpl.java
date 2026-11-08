package com.itsworld.commerce.address.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itsworld.commerce.address.dto.AddressSaveRequest;
import com.itsworld.commerce.address.entity.UserShippingAddress;
import com.itsworld.commerce.address.mapper.UserShippingAddressMapper;
import com.itsworld.commerce.address.service.AddressService;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收货地址业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserShippingAddressMapper addressMapper;

    @Override
    public List<UserShippingAddress> listAddresses(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<UserShippingAddress>()
                .eq(UserShippingAddress::getUserId, userId)
                .orderByDesc(UserShippingAddress::getIsDefault)
                .orderByDesc(UserShippingAddress::getId));
    }

    @Override
    @Transactional
    public UserShippingAddress createAddress(Long userId, AddressSaveRequest request) {
        boolean first = addressMapper.selectCount(new LambdaQueryWrapper<UserShippingAddress>()
                .eq(UserShippingAddress::getUserId, userId)) == 0;
        int isDefault = request.getIsDefault() != null ? request.getIsDefault() : (first ? 1 : 0);
        if (isDefault == 1) {
            clearDefault(userId);
        }
        UserShippingAddress address = UserShippingAddress.builder()
                .userId(userId)
                .recipientName(request.getRecipientName().trim())
                .mobile(request.getMobile().trim())
                .province(request.getProvince())
                .city(request.getCity())
                .district(request.getDistrict())
                .addressDetail(request.getAddressDetail().trim())
                .isDefault(isDefault)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        addressMapper.insert(address);
        return address;
    }

    @Override
    @Transactional
    public UserShippingAddress updateAddress(Long userId, Long addressId, AddressSaveRequest request) {
        UserShippingAddress address = getOwnedAddress(userId, addressId);
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            clearDefault(userId);
            address.setIsDefault(1);
        } else if (request.getIsDefault() != null) {
            address.setIsDefault(0);
        }
        address.setRecipientName(request.getRecipientName().trim());
        address.setMobile(request.getMobile().trim());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setAddressDetail(request.getAddressDetail().trim());
        address.setUpdatedAt(LocalDateTime.now());
        addressMapper.updateById(address);
        return address;
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        getOwnedAddress(userId, addressId);
        addressMapper.deleteById(addressId);
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long addressId) {
        getOwnedAddress(userId, addressId);
        clearDefault(userId);
        addressMapper.update(null, new LambdaUpdateWrapper<UserShippingAddress>()
                .eq(UserShippingAddress::getId, addressId)
                .eq(UserShippingAddress::getUserId, userId)
                .set(UserShippingAddress::getIsDefault, 1)
                .set(UserShippingAddress::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public UserShippingAddress getOwnedAddress(Long userId, Long addressId) {
        UserShippingAddress address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "收货地址不存在");
        }
        return address;
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null, new LambdaUpdateWrapper<UserShippingAddress>()
                .eq(UserShippingAddress::getUserId, userId)
                .set(UserShippingAddress::getIsDefault, 0)
                .set(UserShippingAddress::getUpdatedAt, LocalDateTime.now()));
    }

}
