package com.itsworld.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itsworld.common.constant.AppConstant;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.util.JwtUtils;
import com.itsworld.security.context.SecurityContextUtils;
import com.itsworld.user.dto.UserLoginRequest;
import com.itsworld.user.dto.UserLoginResponse;
import com.itsworld.user.dto.UserRegisterRequest;
import com.itsworld.user.dto.UserResponse;
import com.itsworld.user.entity.User;
import com.itsworld.user.mapper.UserMapper;
import com.itsworld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户业务实现
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * 用户注册
     */
    @Override
    @Transactional
    public Long register(UserRegisterRequest registerRequest) {
        // 1. 验证密码确认是否一致
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 2. 验证密码长度
        if (registerRequest.getPassword().length() < AppConstant.PASSWORD_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        // 3. 检查用户名是否已存在
        User existingUserByUsername = userMapper.selectByUsername(registerRequest.getUsername());
        if (existingUserByUsername != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 4. 邮箱空串归一为 null，避免唯一索引冲突
        String email = registerRequest.getEmail();
        if (email != null && email.isBlank()) {
            email = null;
        }

        // 5. 检查邮箱是否已存在（如果提供了邮箱）
        if (email != null) {
            User existingUserByEmail = userMapper.selectByEmail(email);
            if (existingUserByEmail != null) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
            }
        }

        // 6. 创建新用户
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(email)
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .nickName(registerRequest.getUsername())  // 默认昵称为用户名
                .status(1)  // 1-正常
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 7. 保存到数据库
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        log.info("用户注册成功: {}", registerRequest.getUsername());
        return user.getId();
    }

    /**
     * 用户登录
     */
    @Override
    public UserLoginResponse login(UserLoginRequest loginRequest) {
        // 1. 查询用户（支持用户名、邮箱、手机号）
        User user = userMapper.selectByUsername(loginRequest.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 验证账户状态
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 4. 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 5. 生成JWT Token
        String token = jwtUtils.generateToken(user.getId().toString(), user.getUsername());

        // 6. 构建登录响应
        UserResponse userResponse = convertToUserResponse(user);
        UserLoginResponse loginResponse = UserLoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(AppConstant.JWT_EXPIRATION)
                .user(userResponse)
                .build();

        log.info("用户登录成功: {}", user.getUsername());
        return loginResponse;
    }

    /**
     * 根据用户名/邮箱/手机号查询用户
     */
    @Override
    public User getUserByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 根据用户ID查询用户
     */
    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 获取当前登录用户信息
     */
    @Override
    public UserResponse getCurrentUserInfo() {
        String username = SecurityContextUtils.getCurrentUsername();
        if (username == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return convertToUserResponse(user);
    }

    /**
     * 更新用户信息
     */
    @Override
    @Transactional
    public UserResponse updateUserInfo(Long userId, UserResponse userRequest) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 更新允许修改的字段
        if (userRequest.getNickName() != null) {
            user.setNickName(userRequest.getNickName());
        }
        if (userRequest.getAvatarUrl() != null) {
            user.setAvatarUrl(userRequest.getAvatarUrl());
        }
        if (userRequest.getBio() != null) {
            user.setBio(userRequest.getBio());
        }
        if (userRequest.getGender() != null) {
            user.setGender(userRequest.getGender());
        }
        if (userRequest.getRegion() != null) {
            user.setRegion(userRequest.getRegion());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户信息更新成功: {}", user.getUsername());
        return convertToUserResponse(user);
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 验证新密码长度
        if (newPassword.length() < AppConstant.PASSWORD_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户密码修改成功: {}", user.getUsername());
    }

    /**
     * 验证密码是否正确
     */
    @Override
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 将User实体转换为UserResponse DTO
     */
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickName(user.getNickName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .gender(user.getGender())
                .region(user.getRegion())
                .build();
    }

}

