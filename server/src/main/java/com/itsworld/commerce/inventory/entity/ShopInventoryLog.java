package com.itsworld.commerce.inventory.entity;

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
 * 库存变动流水实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("shop_inventory_log")
public class ShopInventoryLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long skuId;

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
