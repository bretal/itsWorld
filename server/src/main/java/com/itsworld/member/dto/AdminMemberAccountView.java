package com.itsworld.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端会员账户列表DTO
 * 以 sys_user 为主表，未开通会员/钱包的用户也可见。
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMemberAccountView {

    private Long userId;

    private String username;

    private String nickName;

    private String phone;

    private String email;

    private String role;

    private Integer status;

    private LocalDateTime lastLoginAt;

    private Long levelId;

    private String levelName;

    private Integer growthValue;

    private LocalDateTime memberExpiresAt;


    private Long principalBalance;

    private Long bonusBalance;

    private Long availableBalance;

    private LocalDateTime createdAt;

}
