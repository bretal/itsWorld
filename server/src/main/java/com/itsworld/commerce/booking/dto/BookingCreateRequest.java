package com.itsworld.commerce.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建美容预约请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreateRequest {

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @NotNull(message = "服务ID不能为空")
    private Long serviceId;

    @NotNull(message = "宠物ID不能为空")
    private Long petId;

    @NotNull(message = "时段ID不能为空")
    private Long slotId;

    /**
     * ONLINE-在线全额预付；PACKAGE-套餐次数核销
     */
    @NotBlank(message = "支付方式不能为空")
    private String payType;

    /**
     * 套餐支付时必填
     */
    private Long userPackageItemId;

    /**
     * 在线支付可选优惠券
     */
    private Long couponId;

    /**
     * 在线支付幂等键，可选；不传由服务端生成
     */
    private String idempotencyKey;

}
