package com.itsworld.commerce.aftersale.controller;

import com.itsworld.commerce.aftersale.dto.AfterSaleApplyRequest;
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
 * 用户端售后控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce/after-sales")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public Result<AfterSaleView> apply(@Valid @RequestBody AfterSaleApplyRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(afterSaleService.apply(userId, request), "售后申请已提交");
    }

    @GetMapping
    public Result<PageResult<AfterSaleView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        Long userId = currentUserService.getUserId();
        return Result.success(afterSaleService.pageUserAfterSales(userId, status,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/{afterSaleNo}")
    public Result<AfterSaleView> detail(@PathVariable String afterSaleNo) {
        Long userId = currentUserService.getUserId();
        return Result.success(afterSaleService.getUserAfterSale(userId, afterSaleNo));
    }

}
