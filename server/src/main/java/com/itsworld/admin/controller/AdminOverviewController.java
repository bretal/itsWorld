package com.itsworld.admin.controller;

import com.itsworld.admin.dto.AdminOverviewResponse;
import com.itsworld.admin.service.AdminOverviewService;
import com.itsworld.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台工作台控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOverviewController {

    private final AdminOverviewService adminOverviewService;

    /**
     * 工作台总览
     */
    @GetMapping("/overview")
    public Result<AdminOverviewResponse> getOverview() {
        return Result.success(adminOverviewService.getOverview());
    }

}
