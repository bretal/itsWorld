package com.itsworld.commerce.order.entity;

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
 * 商业订单项实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("commerce_order_item")
public class CommerceOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String itemType;

    private Long targetId;

    private Long skuId;

    private Long petId;

    private Integer quantity;

    private String titleSnapshot;

    private String specSnapshot;

    private Long unitPrice;

    private Long originalTotal;

    private Long discountAllocated;

    private Long payableTotal;

    private Integer fulfilledQty;

    private Long refundedAmount;

    private LocalDateTime createdAt;

}
