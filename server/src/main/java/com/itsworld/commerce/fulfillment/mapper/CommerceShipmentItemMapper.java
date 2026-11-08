package com.itsworld.commerce.fulfillment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.fulfillment.entity.CommerceShipmentItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商业发货明细数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommerceShipmentItemMapper extends BaseMapper<CommerceShipmentItem> {
}
