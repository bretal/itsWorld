package com.itsworld.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.community.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评论数据访问接口
 *
 * @author Claude
 * @date 2026-10-30
 */
@Mapper
public interface CommentMapper extends BaseMapper<PostComment> {

    /**
     * 根据帖子ID获取评论列表
     *
     * @param postId 帖子ID
     * @return 评论列表
     */
    @Select("SELECT id, post_id, user_id, parent_comment_id AS replyToCommentId, content, like_count, created_at, deleted_at FROM community_comment WHERE post_id = #{postId} AND deleted_at IS NULL ORDER BY created_at DESC")
    List<PostComment> selectByPostId(Long postId);

    /**
     * 获取一级评论列表（不包括回复）
     *
     * @param postId 帖子ID
     * @return 一级评论列表
     */
    @Select("SELECT id, post_id, user_id, parent_comment_id AS replyToCommentId, content, like_count, created_at, deleted_at FROM community_comment WHERE post_id = #{postId} AND parent_comment_id IS NULL AND deleted_at IS NULL ORDER BY created_at DESC")
    List<PostComment> selectRootComments(Long postId);

    /**
     * 获取对某条评论的回复
     *
     * @param replyToCommentId 被回复的评论ID
     * @return 回复列表
     */
    @Select("SELECT id, post_id, user_id, parent_comment_id AS replyToCommentId, content, like_count, created_at, deleted_at FROM community_comment WHERE parent_comment_id = #{replyToCommentId} AND deleted_at IS NULL ORDER BY created_at ASC")
    List<PostComment> selectReplies(Long replyToCommentId);

}
