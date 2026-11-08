package com.itsworld.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 * 对应数据库表 sys_user
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码哈希（BCrypt加密）
     */
    private String passwordHash;

    /**
     * 密码盐值（预留）
     */
    private String passwordSalt;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 头像URL（MinIO存储路径）
     */
    private String avatarUrl;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 性别 (0-未设置 1-男 2-女)
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private String birthDate;

    /**
     * 地区
     */
    private String region;

    /**
     * 账户状态 (1-正常 0-禁用)
     */
    private Integer status;

    /**
     * 角色 (USER-普通用户 ADMIN-运营管理员)
     */
    private String role;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    private LocalDateTime deletedAt;

}
