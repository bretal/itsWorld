package com.itsworld.common.util;

import com.itsworld.common.constant.AppConstant;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 负责JWT token的生成、验证和解析
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * JWT签名密钥（从application.yml读取）
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT过期时间（从application.yml读取，单位：秒）
     */
    @Value("${jwt.expiration:86400}")
    private Long expiration;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成JWT Token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT token字符串
     */
    public String generateToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        Date nowDate = new Date();
        Date expireDate = new Date(nowDate.getTime() + expiration * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(nowDate)
                .setExpiration(expireDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 验证JWT Token是否有效
     *
     * @param token JWT token字符串
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("JWT签名验证失败：{}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT格式不正确：{}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT已过期：{}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT：{}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT声明为空：{}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT验证异常：{}", e.getMessage());
        }
        return false;
    }

    /**
     * 解析JWT Token
     *
     * @param token JWT token字符串
     * @return JWT声明对象
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("JWT解析异常：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT token字符串
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("userId", String.class);
        }
        return null;
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT token字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("username", String.class);
        }
        return null;
    }

    /**
     * 刷新JWT Token
     *
     * @param token 旧的JWT token
     * @return 新的JWT token
     */
    public String refreshToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            String userId = claims.get("userId", String.class);
            String username = claims.get("username", String.class);
            return generateToken(userId, username);
        }
        return null;
    }

    /**
     * 提取Authorization Header中的JWT Token
     *
     * @param authHeader Authorization header值
     * @return JWT token字符串（去掉Bearer前缀）
     */
    public static String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith(AppConstant.JWT_PREFIX)) {
            return authHeader.substring(AppConstant.JWT_PREFIX.length());
        }
        return null;
    }

}

