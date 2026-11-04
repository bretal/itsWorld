package com.itsworld.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子响应DTO
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    /**
     * 帖子ID
     */
    private Long id;

    /**
     * 发布者用户ID
     */
    private Long userId;

    /**
     * 发布者用户名
     */
    private String username;

    /**
     * 发布者头像
     */
    private String userAvatar;

    /**
     * 相关宠物ID
     */
    private Long petId;

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 照片URL列表
     */
    private List<String> photoUrls;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 当前用户是否点赞
     */
    private Boolean isLiked;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
