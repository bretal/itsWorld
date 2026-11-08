package com.itsworld.commerce.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价展示
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewView {

    private Long id;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private String username;

    private Integer rating;

    private String content;

    private List<String> images;

    private String status;

    private String reply;

    private LocalDateTime repliedAt;

    private LocalDateTime createdAt;

}
