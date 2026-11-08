package com.itsworld.commerce.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.review.entity.CommerceReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商业订单评价数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommerceReviewMapper extends BaseMapper<CommerceReview> {

    @Select("SELECT * FROM commerce_review WHERE order_id = #{orderId} LIMIT 1")
    CommerceReview selectByOrderId(@Param("orderId") Long orderId);

}
