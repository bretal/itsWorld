package com.itsworld.commerce.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 库存流水展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLogView {

    private Long id;

    private Long skuId;

    private String skuCode;

    private String productName;

    private String bizType;

    private Long bizId;

    private Integer changeAvailable;

    private Integer changeLocked;

    private Integer beforeAvailable;

    private Integer afterAvailable;

    private String idempotencyKey;

    private String remark;

    private LocalDateTime createdAt;

}
