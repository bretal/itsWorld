package com.itsworld.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.community.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 帖子数据访问接口
 *
 * @author Claude
 * @date 2026-10-30
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 根据用户ID获取帖子列表
     *
     * @param userId 用户ID
     * @return 帖子列表
     */
    @Select("SELECT id, user_id, pet_id, title, content, images_json AS photoUrls, category, like_count, comment_count, view_count, status, created_at, updated_at, deleted_at FROM community_post WHERE user_id = #{userId} AND deleted_at IS NULL ORDER BY created_at DESC")
    List<Post> selectByUserId(Long userId);

    /**
     * 获取热门帖子（按点赞数排序）
     *
     * @return 热门帖子列表
     */
    @Select("SELECT id, user_id, pet_id, title, content, images_json AS photoUrls, category, like_count, comment_count, view_count, status, created_at, updated_at, deleted_at FROM community_post WHERE status = 1 AND deleted_at IS NULL ORDER BY like_count DESC, created_at DESC LIMIT 20")
    List<Post> selectHotPosts();

    /**
     * 获取推荐帖子（混合排序）
     *
     * @return 推荐帖子列表
     */
    @Select("SELECT id, user_id, pet_id, title, content, images_json AS photoUrls, category, like_count, comment_count, view_count, status, created_at, updated_at, deleted_at FROM community_post WHERE status = 1 AND deleted_at IS NULL ORDER BY like_count DESC, created_at DESC LIMIT 20")
    List<Post> selectRecommendPosts();

}
