package com.itsworld.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 帖子创建请求DTO
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequest {

    /**
     * 帖子标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 帖子内容
     */
    @NotBlank(message = "内容不能为空")
    private String content;

    /**
     * 相关宠物ID
     */
    private Long petId;

    /**
     * 照片URL列表
     */
    private List<String> photoUrls;

}
