package com.itsworld.community.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.community.dto.CommentRequest;
import com.itsworld.community.dto.PostCreateRequest;
import com.itsworld.community.dto.PostResponse;
import com.itsworld.community.entity.Follow;
import com.itsworld.community.entity.Post;
import com.itsworld.community.entity.PostComment;
import com.itsworld.community.mapper.CommentMapper;
import com.itsworld.community.mapper.FollowMapper;
import com.itsworld.community.mapper.PostMapper;
import com.itsworld.community.service.CommunityService;
import com.itsworld.user.entity.User;
import com.itsworld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 社区业务实现
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final FollowMapper followMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    /**
     * 发布帖子
     */
    @Override
    @Transactional
    public Long createPost(Long userId, PostCreateRequest createRequest) {
        // 1. 序列化照片列表
        String photoUrlsJson = null;
        if (createRequest.getPhotoUrls() != null && !createRequest.getPhotoUrls().isEmpty()) {
            try {
                photoUrlsJson = objectMapper.writeValueAsString(createRequest.getPhotoUrls());
            } catch (Exception e) {
                log.warn("序列化照片列表失败", e);
                photoUrlsJson = "[]";
            }
        }

        // 2. 创建帖子
        Post post = Post.builder()
                .userId(userId)
                .petId(createRequest.getPetId())
                .title(createRequest.getTitle())
                .content(createRequest.getContent())
                .photoUrls(photoUrlsJson)
                .likeCount(0)
                .commentCount(0)
                .viewCount(0)
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 3. 保存到数据库
        int result = postMapper.insert(post);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        log.info("帖子发布成功: userId={}, postId={}", userId, post.getId());
        return post.getId();
    }

    /**
     * 获取用户的帖子列表
     */
    @Override
    public List<PostResponse> getUserPosts(Long userId) {
        List<Post> posts = postMapper.selectByUserId(userId);
        User user = userService.getUserById(userId);

        return posts.stream()
                .map(post -> convertToPostResponse(post, user, userId))
                .collect(Collectors.toList());
    }

    /**
     * 获取帖子详情
     */
    @Override
    public PostResponse getPostInfo(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null || Integer.valueOf(0).equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        User user = userService.getUserById(post.getUserId());
        return convertToPostResponse(post, user, currentUserId);
    }

    /**
     * 获取热门帖子
     */
    @Override
    public List<PostResponse> getHotPosts(Long currentUserId) {
        List<Post> posts = postMapper.selectHotPosts();
        return posts.stream()
                .map(post -> {
                    User user = userService.getUserById(post.getUserId());
                    return convertToPostResponse(post, user, currentUserId);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取推荐帖子
     */
    @Override
    public List<PostResponse> getRecommendPosts(Long currentUserId) {
        List<Post> posts = postMapper.selectRecommendPosts();
        return posts.stream()
                .map(post -> {
                    User user = userService.getUserById(post.getUserId());
                    return convertToPostResponse(post, user, currentUserId);
                })
                .collect(Collectors.toList());
    }

    /**
     * 删除帖子
     */
    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 逻辑删除（deleted_at）
        postMapper.deleteById(postId);

        log.info("帖子删除成功: userId={}, postId={}", userId, postId);
    }

    /**
     * 点赞帖子
     */
    @Override
    @Transactional
    public void likePost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // TODO: 需要创建post_like表来记录用户点赞记录
        post.setLikeCount((post.getLikeCount() != null ? post.getLikeCount() : 0) + 1);
        postMapper.updateById(post);

        log.info("帖子点赞成功: userId={}, postId={}", userId, postId);
    }

    /**
     * 取消点赞
     */
    @Override
    @Transactional
    public void unlikePost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // TODO: 检查用户是否已点赞，取消点赞
        if (post.getLikeCount() != null && post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
            postMapper.updateById(post);
        }

        log.info("帖子取消点赞成功: userId={}, postId={}", userId, postId);
    }

    /**
     * 发表评论
     */
    @Override
    @Transactional
    public Long createComment(Long userId, Long postId, CommentRequest commentRequest) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 创建评论
        PostComment comment = PostComment.builder()
                .postId(postId)
                .userId(userId)
                .replyToCommentId(commentRequest.getReplyToCommentId())
                .content(commentRequest.getContent())
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        int result = commentMapper.insert(comment);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        // 更新帖子的评论数
        post.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
        postMapper.updateById(post);

        log.info("评论发表成功: userId={}, postId={}, commentId={}", userId, postId, comment.getId());
        return comment.getId();
    }

    /**
     * 删除评论
     */
    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        PostComment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 逻辑删除（deleted_at）
        commentMapper.deleteById(commentId);

        // 更新帖子的评论数
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() != null && post.getCommentCount() > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
            postMapper.updateById(post);
        }

        log.info("评论删除成功: userId={}, commentId={}", userId, commentId);
    }

    /**
     * 关注用户
     */
    @Override
    @Transactional
    public void followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "不能关注自己");
        }

        // 检查是否已关注
        Integer count = followMapper.checkFollowing(followerId, followingId);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "已经关注过了");
        }

        // 创建关注记录
        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .createdAt(LocalDateTime.now())
                .build();

        int result = followMapper.insert(follow);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        log.info("关注用户成功: followerId={}, followingId={}", followerId, followingId);
    }

    /**
     * 取消关注
     */
    @Override
    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        // user_follow 没有 deleted_at 列，采用硬删除；唯一键允许之后重新关注
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Follow> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("follower_id", followerId)
                .eq("following_id", followingId);
        followMapper.delete(wrapper);

        log.info("取消关注成功: followerId={}, followingId={}", followerId, followingId);
    }

    /**
     * 获取关注列表
     */
    @Override
    public List<Long> getFollowingList(Long userId) {
        List<Follow> followList = followMapper.selectFollowing(userId);
        return followList.stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toList());
    }

    /**
     * 获取粉丝列表
     */
    @Override
    public List<Long> getFollowerList(Long userId) {
        List<Follow> followList = followMapper.selectFollowers(userId);
        return followList.stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
    }

    /**
     * 获取粉丝数
     */
    @Override
    public Integer getFollowerCount(Long userId) {
        Integer count = followMapper.getFollowerCount(userId);
        return count != null ? count : 0;
    }

    /**
     * 获取关注数
     */
    @Override
    public Integer getFollowingCount(Long userId) {
        Integer count = followMapper.getFollowingCount(userId);
        return count != null ? count : 0;
    }

    /**
     * 将Post实体转换为PostResponse DTO
     */
    private PostResponse convertToPostResponse(Post post, User user, Long currentUserId) {
        // 反序列化照片列表
        List<String> photoUrls = null;
        if (post.getPhotoUrls() != null && !post.getPhotoUrls().isEmpty() && !post.getPhotoUrls().equals("[]")) {
            try {
                photoUrls = objectMapper.readValue(post.getPhotoUrls(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("反序列化照片列表失败", e);
                photoUrls = List.of();
            }
        }

        // TODO: 检查当前用户是否点赞了该帖子
        Boolean isLiked = false;

        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .username(user.getUsername())
                .userAvatar(user.getAvatarUrl())
                .petId(post.getPetId())
                .title(post.getTitle())
                .content(post.getContent())
                .photoUrls(photoUrls)
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isLiked(isLiked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

}
