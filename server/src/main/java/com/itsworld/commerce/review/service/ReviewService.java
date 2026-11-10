package com.itsworld.commerce.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.commerce.order.entity.CommerceOrder;
import com.itsworld.commerce.order.mapper.CommerceOrderMapper;
import com.itsworld.commerce.review.dto.ReviewSubmitRequest;
import com.itsworld.commerce.review.dto.ReviewView;
import com.itsworld.commerce.review.entity.CommerceReview;
import com.itsworld.commerce.review.mapper.CommerceReviewMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.user.entity.User;
import com.itsworld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单评价业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final CommerceReviewMapper reviewMapper;
    private final CommerceOrderMapper orderMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReviewView submit(Long userId, ReviewSubmitRequest request) {
        CommerceOrder order = orderMapper.selectByOrderNo(request.getOrderNo());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if (!"COMPLETED".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED.getCode(),
                    "订单完成后才能评价");
        }
        if (reviewMapper.selectByOrderId(order.getId()) != null) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now();
        CommerceReview review = CommerceReview.builder()
                .orderId(order.getId())
                .userId(userId)
                .rating(request.getRating())
                .content(request.getContent())
                .images(toJson(request.getImages()))
                .status("PENDING")
                .createdAt(now)
                .updatedAt(now)
                .build();
        reviewMapper.insert(review);
        log.info("订单评价提交: orderNo={}, userId={}, rating={}",
                request.getOrderNo(), userId, request.getRating());
        return buildView(review);
    }

    public ReviewView getByOrder(Long userId, String orderNo) {
        CommerceOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        CommerceReview review = reviewMapper.selectByOrderId(order.getId());
        return review != null ? buildView(review) : null;
    }

    public PageResult<ReviewView> pageAdminReviews(String status, long pageNum, long pageSize) {
        Page<CommerceReview> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CommerceReview> wrapper = new LambdaQueryWrapper<CommerceReview>()
                .orderByDesc(CommerceReview::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(CommerceReview::getStatus, status.trim().toUpperCase());
        }
        Page<CommerceReview> result = reviewMapper.selectPage(page, wrapper);
        List<ReviewView> records = new ArrayList<>();
        for (CommerceReview review : result.getRecords()) {
            records.add(buildView(review));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public ReviewView approve(Long reviewId, Long adminId) {
        CommerceReview review = requireReview(reviewId);
        if (!"PENDING".equals(review.getStatus())) {
            throw new BusinessException(ErrorCode.REVIEW_STATUS_ERROR);
        }
        review.setStatus("APPROVED");
        review.setUpdatedAt(LocalDateTime.now());
        reviewMapper.updateById(review);
        log.info("评价审核通过: reviewId={}, adminId={}", reviewId, adminId);
        return buildView(review);
    }

    @Transactional
    public ReviewView hide(Long reviewId, Long adminId) {
        CommerceReview review = requireReview(reviewId);
        if ("HIDDEN".equals(review.getStatus())) {
            return buildView(review);
        }
        review.setStatus("HIDDEN");
        review.setUpdatedAt(LocalDateTime.now());
        reviewMapper.updateById(review);
        log.info("评价隐藏: reviewId={}, adminId={}", reviewId, adminId);
        return buildView(review);
    }

    @Transactional
    public ReviewView reply(Long reviewId, String reply) {
        CommerceReview review = requireReview(reviewId);
        review.setReply(reply.trim());
        review.setRepliedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        reviewMapper.updateById(review);
        return buildView(review);
    }

    private CommerceReview requireReview(Long reviewId) {
        CommerceReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }
        return review;
    }

    private ReviewView buildView(CommerceReview review) {
        CommerceOrder order = orderMapper.selectById(review.getOrderId());
        User user = userService.getUserById(review.getUserId());
        return ReviewView.builder()
                .id(review.getId())
                .orderId(review.getOrderId())
                .orderNo(order != null ? order.getOrderNo() : null)
                .userId(review.getUserId())
                .username(user != null ? user.getUsername() : null)
                .rating(review.getRating())
                .content(review.getContent())
                .images(parseImages(review.getImages()))
                .status(review.getStatus())
                .reply(review.getReply())
                .repliedAt(review.getRepliedAt())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private List<String> parseImages(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("评价图片解析失败: {}", value);
            return List.of();
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "评价图片序列化失败");
        }
    }

}
