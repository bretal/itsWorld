package com.itsworld.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 评论请求/响应DTO
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequest {

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    private String content;

    /**
     * 回复的评论ID
     */
    private Long replyToCommentId;

}

/**
 * 评论响应DTO
 */
@Data
class CommentResponse {

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 评论者用户ID
     */
    private Long userId;

    /**
     * 评论者用户名
     */
    private String username;

    /**
     * 评论者头像
     */
    private String userAvatar;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 回复的评论ID
     */
    private Long replyToCommentId;

    /**
     * 被回复的评论者用户名
     */
    private String replyToUsername;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 当前用户是否点赞
     */
    private Boolean isLiked;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
