package com.itsworld.security.filter;

import com.itsworld.common.constant.AppConstant;
import com.itsworld.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器
 * 拦截每个请求，验证JWT Token的有效性
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 从Authorization Header中提取JWT Token
            String authHeader = request.getHeader(AppConstant.JWT_HEADER);
            String token = JwtUtils.extractTokenFromHeader(authHeader);

            // 2. 验证Token的有效性
            if (token != null && jwtUtils.validateToken(token)) {
                // 3. 解析Token获取用户信息
                Claims claims = jwtUtils.parseToken(token);
                String userId = claims.get("userId", String.class);
                String username = claims.get("username", String.class);

                if (StringUtils.hasText(username)) {
                    // 4. 从数据库加载用户信息
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 5. 创建Authentication对象
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 6. 将Authentication存入SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("用户认证成功：username={}, userId={}", username, userId);
                }
            }

        } catch (Exception e) {
            log.error("JWT认证异常：{}", e.getMessage());
        }

        // 7. 继续处理请求
        filterChain.doFilter(request, response);
    }

}
