package com.itsworld.commerce.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.catalog.entity.ShopCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface ShopCategoryMapper extends BaseMapper<ShopCategory> {
}
