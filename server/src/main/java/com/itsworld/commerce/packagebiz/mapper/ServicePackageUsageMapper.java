package com.itsworld.commerce.packagebiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.packagebiz.entity.ServicePackageUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 套餐使用核销数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface ServicePackageUsageMapper extends BaseMapper<ServicePackageUsage> {

    @Select("SELECT * FROM service_package_usage WHERE booking_id = #{bookingId} "
            + "AND status = 'FROZEN' ORDER BY id DESC LIMIT 1")
    ServicePackageUsage selectFrozenByBookingId(@Param("bookingId") Long bookingId);

    @Select("SELECT * FROM service_package_usage WHERE user_package_id = #{userPackageId} "
            + "ORDER BY id DESC")
    List<ServicePackageUsage> selectByUserPackageId(@Param("userPackageId") Long userPackageId);

    @Update("UPDATE service_package_usage SET status = 'USED', used_time = NOW(), "
            + "updated_at = NOW() WHERE id = #{usageId} AND status = 'FROZEN'")
    int markUsed(@Param("usageId") Long usageId);

    @Update("UPDATE service_package_usage SET status = 'RELEASED', updated_at = NOW() "
            + "WHERE id = #{usageId} AND status = 'FROZEN'")
    int markReleased(@Param("usageId") Long usageId);

    @Select("SELECT * FROM service_package_usage WHERE idempotency_key = #{key} LIMIT 1")
    ServicePackageUsage selectByIdempotencyKey(@Param("key") String key);

}
