package com.itsworld.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日记响应DTO
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryResponse {

    /**
     * 日记ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 日记日期
     */
    private String diaryDate;

    /**
     * 日记分类
     */
    private String category;

    /**
     * 问候标题
     */
    private String greeting;

    /**
     * 六步问候得分（0-12）
     */
    private Integer healthScore;

    /**
     * 日记内容
     */
    private String content;

    /**
     * 附加照片URL列表
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
     * 是否公开
     */
    private Integer isPublic;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
