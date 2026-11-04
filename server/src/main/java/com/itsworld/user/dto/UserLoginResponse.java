package com.itsworld.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应DTO（包含JWT Token）
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginResponse {

    /**
     * JWT Token
     */
    private String token;

    /**
     * Token类型（Bearer）
     */
    private String tokenType = "Bearer";

    /**
     * Token过期时间（秒）
     */
    private Long expiresIn = 86400L;

    /**
     * 用户信息
     */
    private UserResponse user;

}
