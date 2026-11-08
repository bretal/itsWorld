package com.itsworld.commerce.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.catalog.entity.ShopProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface ShopProductMapper extends BaseMapper<ShopProduct> {
}
