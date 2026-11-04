package com.itsworld.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问接口
 * 继承MyBatis-Plus的BaseMapper，可直接使用CRUD方法
 *
 * @author Claude
 * @date 2026-10-30
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     * 支持用户名、邮箱、手机号查询
     *
     * @param username 用户名/邮箱/手机号
     * @return 用户对象
     */
    @Select("SELECT * FROM sys_user WHERE (username = #{username} OR email = #{username} OR phone = #{username}) AND deleted_at IS NULL LIMIT 1")
    User selectByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户对象
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted_at IS NULL LIMIT 1")
    User selectByEmail(String email);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户对象
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND deleted_at IS NULL LIMIT 1")
    User selectByPhone(String phone);

}
