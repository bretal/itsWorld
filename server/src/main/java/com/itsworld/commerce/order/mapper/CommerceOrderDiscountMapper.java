package com.itsworld.commerce.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.order.entity.CommerceOrderDiscount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商业订单优惠明细数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommerceOrderDiscountMapper extends BaseMapper<CommerceOrderDiscount> {
}
