package com.itsworld.commerce.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评价回复请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复不能超过500字")
    private String reply;

}
