package com.itsworld.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 日记创建请求DTO
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryCreateRequest {

    /**
     * 宠物ID
     */
    @NotNull(message = "宠物ID不能为空")
    private Long petId;

    /**
     * 日记日期
     */
    @NotBlank(message = "日期不能为空")
    private String diaryDate;

    /**
     * 日记分类（daily-日常, food-饮食, health-健康）
     */
    @Pattern(regexp = "daily|food|health", message = "日记分类不合法")
    private String category;

    /**
     * 位置
     */
    private String location;

    /**
     * 心情
     */
    private String mood;

    /**
     * 当前活动
     */
    private List<String> activities;

    /**
     * 健康评分（0-12）
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
     * 是否公开
     */
    private Integer isPublic;

}
