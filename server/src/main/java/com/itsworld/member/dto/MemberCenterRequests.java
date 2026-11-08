package com.itsworld.member.dto;

import jakarta.validation.constraints.*;

/** 会员中心写入请求。金额以分计，期限以自然日计。 */
public final class MemberCenterRequests {
    private MemberCenterRequests() { }

    public record Plan(@NotBlank @Size(max=100) String name, @NotNull Long levelId,
                       @NotNull @Min(1) Long price, @NotNull @Min(1) @Max(3650) Integer durationDays,
                       @NotNull @Min(0) @Max(1) Integer status) { }
    public record Purchase(@NotNull Long planId, @NotBlank @Size(max=64) String requestKey) { }
    public record Reward(@NotBlank @Size(max=100) String name, @NotNull @Min(1) Integer growthCost,
                         @NotNull @Min(0) Integer stock, @NotNull @Min(0) @Max(1) Integer status) { }
    public record Redeem(@NotNull Long productId, @NotBlank @Size(max=64) String requestKey) { }
}
