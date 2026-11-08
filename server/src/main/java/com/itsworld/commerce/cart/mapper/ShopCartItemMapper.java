package com.itsworld.commerce.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.cart.entity.ShopCartItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface ShopCartItemMapper extends BaseMapper<ShopCartItem> {
}
