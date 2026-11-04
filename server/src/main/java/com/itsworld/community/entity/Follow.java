package com.itsworld.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户关注关系实体
 * 对应数据库表 user_follow
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("user_follow")
public class Follow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关注关系ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关注者用户ID
     */
    private Long followerId;

    /**
     * 被关注者用户ID
     */
    private Long followingId;

    /**
     * 关注时间
     */
    private LocalDateTime createdAt;

}
