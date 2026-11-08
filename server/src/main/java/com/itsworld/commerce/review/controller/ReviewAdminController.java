package com.itsworld.commerce.review.controller;

import com.itsworld.commerce.review.dto.ReviewReplyRequest;
import com.itsworld.commerce.review.dto.ReviewView;
import com.itsworld.commerce.review.service.ReviewService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端评价控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin/commerce/reviews")
@RequiredArgsConstructor
public class ReviewAdminController {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<ReviewView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(reviewService.pageAdminReviews(status,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @PostMapping("/{reviewId}/approve")
    public Result<ReviewView> approve(@PathVariable Long reviewId) {
        return Result.success(reviewService.approve(reviewId, currentUserService.getUserId()),
                "评价已通过");
    }

    @PostMapping("/{reviewId}/hide")
    public Result<ReviewView> hide(@PathVariable Long reviewId) {
        return Result.success(reviewService.hide(reviewId, currentUserService.getUserId()),
                "评价已隐藏");
    }

    @PostMapping("/{reviewId}/reply")
    public Result<ReviewView> reply(@PathVariable Long reviewId,
                                    @Valid @RequestBody ReviewReplyRequest request) {
        return Result.success(reviewService.reply(reviewId, request.getReply()), "回复已提交");
    }

}
