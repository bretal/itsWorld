package com.itsworld.commerce.packagebiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.packagebiz.entity.UserServicePackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户已购套餐数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface UserServicePackageMapper extends BaseMapper<UserServicePackage> {

    @Select("SELECT * FROM user_service_package WHERE order_item_id = #{orderItemId} LIMIT 1")
    UserServicePackage selectByOrderItemId(@Param("orderItemId") Long orderItemId);

}
