package com.itsworld.user.controller;

import com.itsworld.common.result.Result;
import com.itsworld.user.dto.UserLoginRequest;
import com.itsworld.user.dto.UserLoginResponse;
import com.itsworld.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台专用登录
 * 校验通过后仅允许 role=ADMIN 的账号进入 Web 管理后台。
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserService userService;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest loginRequest) {
        return Result.success(userService.adminLogin(loginRequest), "登录成功");
    }

}
