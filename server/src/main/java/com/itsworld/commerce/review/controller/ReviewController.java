package com.itsworld.commerce.review.controller;

import com.itsworld.commerce.review.dto.ReviewSubmitRequest;
import com.itsworld.commerce.review.dto.ReviewView;
import com.itsworld.commerce.review.service.ReviewService;
import com.itsworld.common.result.Result;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端评价控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public Result<ReviewView> submit(@Valid @RequestBody ReviewSubmitRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(reviewService.submit(userId, request), "评价已提交");
    }

    @GetMapping("/order/{orderNo}")
    public Result<ReviewView> getByOrder(@PathVariable String orderNo) {
        Long userId = currentUserService.getUserId();
        return Result.success(reviewService.getByOrder(userId, orderNo));
    }

}
