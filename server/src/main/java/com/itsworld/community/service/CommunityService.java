package com.itsworld.community.service;

import com.itsworld.community.dto.CommentRequest;
import com.itsworld.community.dto.PostCreateRequest;
import com.itsworld.community.dto.PostResponse;

import java.util.List;

/**
 * 社区业务接口
 *
 * @author Claude
 * @date 2026-10-30
 */
public interface CommunityService {

    /**
     * 发布帖子
     *
     * @param userId 用户ID
     * @param createRequest 创建请求
     * @return 帖子ID
     */
    Long createPost(Long userId, PostCreateRequest createRequest);

    /**
     * 获取用户的帖子列表
     *
     * @param userId 用户ID
     * @return 帖子列表
     */
    List<PostResponse> getUserPosts(Long userId);

    /**
     * 获取帖子详情
     *
     * @param postId 帖子ID
     * @param currentUserId 当前用户ID
     * @return 帖子信息
     */
    PostResponse getPostInfo(Long postId, Long currentUserId);

    /**
     * 获取热门帖子
     *
     * @param currentUserId 当前用户ID
     * @return 热门帖子列表
     */
    List<PostResponse> getHotPosts(Long currentUserId);

    /**
     * 获取推荐帖子
     *
     * @param currentUserId 当前用户ID
     * @return 推荐帖子列表
     */
    List<PostResponse> getRecommendPosts(Long currentUserId);

    /**
     * 删除帖子
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     */
    void deletePost(Long userId, Long postId);

    /**
     * 点赞帖子
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     */
    void likePost(Long userId, Long postId);

    /**
     * 取消点赞
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     */
    void unlikePost(Long userId, Long postId);

    /**
     * 发表评论
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @param commentRequest 评论请求
     * @return 评论ID
     */
    Long createComment(Long userId, Long postId, CommentRequest commentRequest);

    /**
     * 删除评论
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * 关注用户
     *
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     */
    void followUser(Long followerId, Long followingId);

    /**
     * 取消关注
     *
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     */
    void unfollowUser(Long followerId, Long followingId);

    /**
     * 获取关注列表
     *
     * @param userId 用户ID
     * @return 关注的用户ID列表
     */
    List<Long> getFollowingList(Long userId);

    /**
     * 获取粉丝列表
     *
     * @param userId 用户ID
     * @return 粉丝用户ID列表
     */
    List<Long> getFollowerList(Long userId);

    /**
     * 获取粉丝数
     *
     * @param userId 用户ID
     * @return 粉丝数量
     */
    Integer getFollowerCount(Long userId);

    /**
     * 获取关注数
     *
     * @param userId 用户ID
     * @return 关注数量
     */
    Integer getFollowingCount(Long userId);

}
