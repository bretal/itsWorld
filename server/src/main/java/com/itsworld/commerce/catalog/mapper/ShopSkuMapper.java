package com.itsworld.commerce.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.catalog.entity.ShopSku;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品SKU数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface ShopSkuMapper extends BaseMapper<ShopSku> {
}
