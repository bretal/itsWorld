package com.itsworld.commerce.review.service;

import com.itsworld.commerce.review.dto.ReviewSubmitRequest;
import com.itsworld.commerce.review.dto.ReviewView;
import com.itsworld.common.result.PageResult;

/**
 * 订单评价业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface ReviewService {

    /**
     * 用户提交评价（订单完成、每单一次）
     */
    ReviewView submit(Long userId, ReviewSubmitRequest request);

    /**
     * 用户查看订单评价，未评价返回 null
     */
    ReviewView getByOrder(Long userId, String orderNo);

    PageResult<ReviewView> pageAdminReviews(String status, long pageNum, long pageSize);

    /**
     * 审核通过
     */
    ReviewView approve(Long reviewId, Long adminId);

    /**
     * 隐藏评价
     */
    ReviewView hide(Long reviewId, Long adminId);

    /**
     * 管理员回复
     */
    ReviewView reply(Long reviewId, String reply);

}
