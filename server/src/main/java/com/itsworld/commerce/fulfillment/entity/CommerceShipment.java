package com.itsworld.commerce.fulfillment.entity;

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
 * 商业发货单实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("commerce_shipment")
public class CommerceShipment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String shipmentNo;

    private Long orderId;

    private String deliveryCompany;

    private String trackingNo;

    private String status;

    private LocalDateTime shippedTime;

    private LocalDateTime receivedTime;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
