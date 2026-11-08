package com.itsworld.user.service;

import com.itsworld.user.dto.UserLoginRequest;
import com.itsworld.user.dto.UserLoginResponse;
import com.itsworld.user.dto.UserRegisterRequest;
import com.itsworld.user.dto.UserResponse;
import com.itsworld.user.entity.User;

/**
 * 用户业务接口
 *
 * @author Claude
 * @date 2026-10-30
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 创建的用户ID
     */
    Long register(UserRegisterRequest registerRequest);

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录响应（包含JWT Token）
     */
    UserLoginResponse login(UserLoginRequest loginRequest);

    /**
     * 管理后台登录（仅 ADMIN）
     *
     * @param loginRequest 登录请求
     * @return 登录响应（包含JWT Token）
     */
    UserLoginResponse adminLogin(UserLoginRequest loginRequest);

    /**
     * 根据用户名/邮箱/手机号查询用户
     *
     * @param username 用户名/邮箱/手机号
     * @return 用户对象
     */
    User getUserByUsername(String username);

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象
     */
    User getUserById(Long userId);

    /**
     * 获取当前登录用户信息
     *
     * @return 用户响应DTO
     */
    UserResponse getCurrentUserInfo();

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param userRequest 用户更新请求
     * @return 更新后的用户信息
     */
    UserResponse updateUserInfo(Long userId, UserResponse userRequest);

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 验证密码是否正确
     *
     * @param rawPassword 原始密码（未加密）
     * @param encodedPassword 加密后的密码
     * @return 密码是否匹配
     */
    boolean verifyPassword(String rawPassword, String encodedPassword);

}
