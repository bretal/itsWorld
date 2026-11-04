package com.itsworld.user.controller;

import com.itsworld.common.result.Result;
import com.itsworld.user.dto.ChangePasswordRequest;
import com.itsworld.user.dto.UserLoginRequest;
import com.itsworld.user.dto.UserLoginResponse;
import com.itsworld.user.dto.UserRegisterRequest;
import com.itsworld.user.dto.UserResponse;
import com.itsworld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户控制器
 * 处理用户注册、登录、信息查询等接口
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     * POST /api/user/register
     *
     * @param registerRequest 注册请求
     * @return 注册成功返回用户ID
     */
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody UserRegisterRequest registerRequest) {
        log.info("用户注册请求: {}", registerRequest.getUsername());
        Long userId = userService.register(registerRequest);
        return Result.success(userId, "注册成功");
    }

    /**
     * 用户登录
     * POST /api/user/login
     *
     * @param loginRequest 登录请求
     * @return 登录成功返回JWT Token和用户信息
     */
    @PostMapping("/login")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest loginRequest) {
        log.info("用户登录请求: {}", loginRequest.getUsername());
        UserLoginResponse loginResponse = userService.login(loginRequest);
        return Result.success(loginResponse, "登录成功");
    }

    /**
     * 获取当前登录用户信息
     * GET /api/user/info
     *
     * @return 当前用户信息
     */
    @GetMapping("/info")
    public Result<UserResponse> getCurrentUserInfo() {
        log.info("查询当前用户信息");
        UserResponse userInfo = userService.getCurrentUserInfo();
        return Result.success(userInfo);
    }

    /**
     * 更新用户信息
     * PUT /api/user/info
     *
     * @param userRequest 用户信息更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/info")
    public Result<UserResponse> updateUserInfo(@RequestBody UserResponse userRequest) {
        log.info("更新用户信息");
        // TODO: 从SecurityContext获取当前用户ID（需在SecurityContextUtils中完成实现）
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        com.itsworld.user.entity.User user = userService.getUserByUsername(username);

        UserResponse updatedUserInfo = userService.updateUserInfo(user.getId(), userRequest);
        return Result.success(updatedUserInfo, "更新成功");
    }

    /**
     * 修改密码
     * POST /api/user/change-password
     *
     * @param request 修改密码请求
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("修改用户密码");
        // TODO: 从SecurityContext获取当前用户ID
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        com.itsworld.user.entity.User user = userService.getUserByUsername(username);

        userService.changePassword(user.getId(), request.getOldPassword(), request.getNewPassword());
        return Result.success(null, "密码修改成功");
    }

}
