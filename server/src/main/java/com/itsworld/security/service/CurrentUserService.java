package com.itsworld.security.service;

import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.user.entity.User;
import com.itsworld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 当前登录用户解析服务
 * 统一封装从 SecurityContext 获取用户名并查询用户实体的逻辑，避免新模块重复两行代码。
 *
 * @author Claude
 * @date 2026-11-07
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserService userService;

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return getUser().getId();
    }

    /**
     * 获取当前登录用户
     *
     * @return 用户实体
     */
    public User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())
                || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userService.getUserByUsername(authentication.getName());
    }

}
