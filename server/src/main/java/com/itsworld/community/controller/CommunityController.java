package com.itsworld.community.controller;

import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.community.dto.CommentRequest;
import com.itsworld.community.dto.CommentResponse;
import com.itsworld.community.dto.PostCreateRequest;
import com.itsworld.community.dto.PostResponse;
import com.itsworld.community.service.CommunityService;
import com.itsworld.user.entity.User;
import com.itsworld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 社区控制器
 * 处理社区相关的操作
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final UserService userService;

    /**
     * 发布帖子
     * POST /api/community
     *
     * @param createRequest 创建请求
     * @return 帖子ID
     */
    @PostMapping
    public Result<Long> createPost(@Valid @RequestBody PostCreateRequest createRequest) {
        log.info("发布帖子: title={}", createRequest.getTitle());

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        Long postId = communityService.createPost(user.getId(), createRequest);
        return Result.success(postId, "帖子发布成功");
    }

    /**
     * 获取用户的帖子列表
     * GET /api/community/user
     *
     * @return 帖子列表
     */
    @GetMapping("/user")
    public Result<List<PostResponse>> getUserPosts() {
        log.info("获取用户帖子列表");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        List<PostResponse> posts = communityService.getUserPosts(user.getId());
        return Result.success(posts);
    }

    /**
     * 获取帖子详情
     * GET /api/community/{postId}
     *
     * @param postId 帖子ID
     * @return 帖子信息
     */
    @GetMapping("/{postId}")
    public Result<PostResponse> getPostInfo(@PathVariable Long postId) {
        log.info("获取帖子详情: postId={}", postId);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        PostResponse post = communityService.getPostInfo(postId, user.getId());
        return Result.success(post);
    }

    /**
     * 获取热门帖子
     * GET /api/community/hot
     *
     * @return 热门帖子列表
     */
    @GetMapping("/hot")
    public Result<List<PostResponse>> getHotPosts() {
        log.info("获取热门帖子");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        List<PostResponse> posts = communityService.getHotPosts(user.getId());
        return Result.success(posts);
    }

    /**
     * 获取推荐帖子
     * GET /api/community/recommend
     *
     * @return 推荐帖子列表
     */
    @GetMapping("/recommend")
    public Result<List<PostResponse>> getRecommendPosts() {
        log.info("获取推荐帖子");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        List<PostResponse> posts = communityService.getRecommendPosts(user.getId());
        return Result.success(posts);
    }

    /**
     * 获取关注流帖子
     * GET /api/community/following/feed
     *
     * @return 关注流帖子列表
     */
    @GetMapping("/following/feed")
    public Result<List<PostResponse>> getFollowingFeed() {
        log.info("获取关注流帖子");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        List<PostResponse> posts = communityService.getFollowingFeed(user.getId());
        return Result.success(posts);
    }

    /**
     * 分页查询社区帖子
     * GET /api/community/page?type=recommend&page=1&size=20
     *
     * @param type hot / recommend / following
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<PageResult<PostResponse>> getPostPage(
            @RequestParam(defaultValue = "recommend") String type,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        log.info("分页查询社区帖子: type={}, page={}, size={}", type, page, size);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        PageResult<PostResponse> result = communityService.getPostPage(
                user.getId(), type,
                PageUtils.normalizePageNum(page),
                PageUtils.normalizePageSize(size));
        return Result.success(result);
    }

    /**
     * 删除帖子
     * DELETE /api/community/{postId}
     *
     * @param postId 帖子ID
     * @return 删除结果
     */
    @DeleteMapping("/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        log.info("删除帖子: postId={}", postId);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        communityService.deletePost(user.getId(), postId);
        return Result.success(null, "帖子删除成功");
    }

    /**
     * 点赞帖子
     * POST /api/community/{postId}/like
     *
     * @param postId 帖子ID
     * @return 点赞结果
     */
    @PostMapping("/{postId}/like")
    public Result<Void> likePost(@PathVariable Long postId) {
        log.info("点赞帖子: postId={}", postId);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        communityService.likePost(user.getId(), postId);
        return Result.success(null, "点赞成功");
    }

    /**
     * 取消点赞
     * DELETE /api/community/{postId}/like
     *
     * @param postId 帖子ID
     * @return 取消点赞结果
     */
    @DeleteMapping("/{postId}/like")
    public Result<Void> unlikePost(@PathVariable Long postId) {
        log.info("取消点赞: postId={}", postId);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        communityService.unlikePost(user.getId(), postId);
        return Result.success(null, "取消点赞成功");
    }

    /**
     * 查询帖子评论列表
     * GET /api/community/{postId}/comments
     *
     * @param postId 帖子ID
     * @return 评论列表
     */
    @GetMapping("/{postId}/comments")
    public Result<List<CommentResponse>> getComments(@PathVariable Long postId) {
        log.info("查询帖子评论: postId={}", postId);
        return Result.success(communityService.getComments(postId));
    }

    /**
     * 分页查询帖子评论
     * GET /api/community/{postId}/comments/page?page=1&size=20
     *
     * @param postId 帖子ID
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    @GetMapping("/{postId}/comments/page")
    public Result<PageResult<CommentResponse>> getCommentPage(
            @PathVariable Long postId,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        log.info("分页查询帖子评论: postId={}, page={}, size={}", postId, page, size);
        PageResult<CommentResponse> result = communityService.getCommentPage(
                postId,
                PageUtils.normalizePageNum(page),
                PageUtils.normalizePageSize(size));
        return Result.success(result);
    }

    /**
     * 发表评论
     * POST /api/community/{postId}/comment
     *
     * @param postId 帖子ID
     * @param commentRequest 评论请求
     * @return 评论ID
     */
    @PostMapping("/{postId}/comment")
    public Result<Long> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest commentRequest) {
        log.info("发表评论: postId={}", postId);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        Long commentId = communityService.createComment(user.getId(), postId, commentRequest);
        return Result.success(commentId, "评论发表成功");
    }

    /**
     * 删除评论
     * DELETE /api/community/comment/{commentId}
     *
     * @param commentId 评论ID
     * @return 删除结果
     */
    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        log.info("删除评论: commentId={}", commentId);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        communityService.deleteComment(user.getId(), commentId);
        return Result.success(null, "评论删除成功");
    }

    /**
     * 关注用户
     * POST /api/community/follow/{userId}
     *
     * @param userId 被关注的用户ID
     * @return 关注结果
     */
    @PostMapping("/follow/{userId}")
    public Result<Void> followUser(@PathVariable Long userId) {
        log.info("关注用户: userId={}", userId);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        communityService.followUser(user.getId(), userId);
        return Result.success(null, "关注成功");
    }

    /**
     * 取消关注
     * DELETE /api/community/follow/{userId}
     *
     * @param userId 被取消关注的用户ID
     * @return 取消关注结果
     */
    @DeleteMapping("/follow/{userId}")
    public Result<Void> unfollowUser(@PathVariable Long userId) {
        log.info("取消关注: userId={}", userId);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        communityService.unfollowUser(user.getId(), userId);
        return Result.success(null, "取消关注成功");
    }

    /**
     * 获取关注列表
     * GET /api/community/following
     *
     * @return 关注的用户ID列表
     */
    @GetMapping("/following")
    public Result<List<Long>> getFollowingList() {
        log.info("获取关注列表");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        List<Long> followingList = communityService.getFollowingList(user.getId());
        return Result.success(followingList);
    }

    /**
     * 获取粉丝列表
     * GET /api/community/followers
     *
     * @return 粉丝用户ID列表
     */
    @GetMapping("/followers")
    public Result<List<Long>> getFollowerList() {
        log.info("获取粉丝列表");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        List<Long> followerList = communityService.getFollowerList(user.getId());
        return Result.success(followerList);
    }

}
