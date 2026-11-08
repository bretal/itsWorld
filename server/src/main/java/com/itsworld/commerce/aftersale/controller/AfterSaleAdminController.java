package com.itsworld.commerce.aftersale.controller;

import com.itsworld.commerce.aftersale.dto.AfterSaleReviewRequest;
import com.itsworld.commerce.aftersale.dto.AfterSaleView;
import com.itsworld.commerce.aftersale.service.AfterSaleService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端售后控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin/commerce/after-sales")
@RequiredArgsConstructor
public class AfterSaleAdminController {

    private final AfterSaleService afterSaleService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<AfterSaleView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(afterSaleService.pageAdminAfterSales(status, keyword,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/{afterSaleNo}")
    public Result<AfterSaleView> detail(@PathVariable String afterSaleNo) {
        return Result.success(afterSaleService.getAdminAfterSale(afterSaleNo));
    }

    @PostMapping("/{afterSaleNo}/review")
    public Result<AfterSaleView> review(@PathVariable String afterSaleNo,
                                        @Valid @RequestBody AfterSaleReviewRequest request) {
        return Result.success(afterSaleService.review(
                afterSaleNo, request, currentUserService.getUserId()), "审核已提交");
    }

    @PostMapping("/{afterSaleNo}/refund")
    public Result<AfterSaleView> refund(@PathVariable String afterSaleNo) {
        return Result.success(afterSaleService.executeRefund(
                afterSaleNo, currentUserService.getUserId()), "退款已完成");
    }

}
