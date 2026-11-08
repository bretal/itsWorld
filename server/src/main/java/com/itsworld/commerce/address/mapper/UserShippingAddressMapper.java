package com.itsworld.commerce.address.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.address.entity.UserShippingAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货地址数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface UserShippingAddressMapper extends BaseMapper<UserShippingAddress> {
}
