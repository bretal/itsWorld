package com.itsworld.commerce.packagebiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.packagebiz.entity.UserServicePackageItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户套餐服务次数数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface UserServicePackageItemMapper extends BaseMapper<UserServicePackageItem> {

    @Update("UPDATE user_service_package_item SET frozen_times = frozen_times + #{qty}, "
            + "updated_at = NOW() WHERE id = #{itemId} "
            + "AND total_times - used_times - frozen_times >= #{qty}")
    int freezeTimes(@Param("itemId") Long itemId, @Param("qty") int qty);

    @Update("UPDATE user_service_package_item SET frozen_times = frozen_times - #{qty}, "
            + "used_times = used_times + #{qty}, updated_at = NOW() WHERE id = #{itemId} "
            + "AND frozen_times >= #{qty}")
    int consumeFrozenTimes(@Param("itemId") Long itemId, @Param("qty") int qty);

    @Update("UPDATE user_service_package_item SET frozen_times = frozen_times - #{qty}, "
            + "updated_at = NOW() WHERE id = #{itemId} AND frozen_times >= #{qty}")
    int releaseFrozenTimes(@Param("itemId") Long itemId, @Param("qty") int qty);

}
