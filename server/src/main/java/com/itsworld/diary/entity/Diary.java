package com.itsworld.diary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宠物日记实体
 * 对应数据库表 pet_diary
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("pet_diary")
public class Diary implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日记ID（自增）
     */
    @TableId(type = IdType.AUTO)
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
     * 日记日期（YYYY-MM-DD）
     */
    private String diaryDate;

    /**
     * 早晨心情
     */
    private String morningMood;

    /**
     * 问题1：位置
     */
    @TableField("question_1")
    private String question1;

    /**
     * 问题2：当前活动
     */
    @TableField("question_2")
    private String question2;

    /**
     * 问题3：健康评分
     */
    @TableField("question_3")
    private String question3;

    /**
     * 问题4
     */
    @TableField("question_4")
    private String question4;

    /**
     * 问题5
     */
    @TableField("question_5")
    private String question5;

    /**
     * 问题6
     */
    @TableField("question_6")
    private String question6;

    /**
     * 每日日记内容
     */
    private String content;

    /**
     * 附加照片URL列表（JSON数组）
     */
    @TableField("images_json")
    private String photoUrls;

    /**
     * 是否发布 (1-已发布 0-草稿)
     */
    @TableField("is_published")
    private Integer isPublic;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
