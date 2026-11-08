package com.itsworld.commerce.aftersale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.aftersale.entity.CommerceRefundLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商业退款记录数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommerceRefundLinkMapper extends BaseMapper<CommerceRefundLink> {

    @Select("SELECT * FROM commerce_refund_link WHERE after_sale_id = #{afterSaleId} "
            + "ORDER BY id DESC LIMIT 1")
    CommerceRefundLink selectLatestByAfterSaleId(@Param("afterSaleId") Long afterSaleId);

}
