package com.itsworld.commerce.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品SPU实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("shop_product")
public class ShopProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productCode;

    private Long categoryId;

    private String name;

    private String subtitle;

    private String description;

    private String coverUrl;

    private String saleStatus;

    private Integer sort;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
