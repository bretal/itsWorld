package com.itsworld.commerce.aftersale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.aftersale.entity.CommerceAfterSale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 商业售后单数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommerceAfterSaleMapper extends BaseMapper<CommerceAfterSale> {

    @Select("SELECT * FROM commerce_after_sale WHERE after_sale_no = #{afterSaleNo} LIMIT 1")
    CommerceAfterSale selectByAfterSaleNo(@Param("afterSaleNo") String afterSaleNo);

    @Update("UPDATE commerce_after_sale SET status = #{toStatus}, review_remark = #{remark}, "
            + "reviewed_by = #{adminId}, reviewed_at = NOW(), updated_at = NOW() "
            + "WHERE id = #{id} AND status = #{fromStatus}")
    int review(@Param("id") Long id, @Param("fromStatus") String fromStatus,
               @Param("toStatus") String toStatus, @Param("remark") String remark,
               @Param("adminId") Long adminId);

    @Update("UPDATE commerce_after_sale SET status = #{status}, refunded_at = #{refundedAt}, "
            + "updated_at = NOW() WHERE id = #{id}")
    int updateRefundStatus(@Param("id") Long id, @Param("status") String status,
                           @Param("refundedAt") java.time.LocalDateTime refundedAt);

}
