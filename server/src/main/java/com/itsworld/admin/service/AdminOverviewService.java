package com.itsworld.admin.service;

import com.itsworld.admin.dto.AdminOverviewResponse;

/**
 * 管理后台总览业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface AdminOverviewService {

    /**
     * 获取工作台总览
     *
     * @return 总览数据
     */
    AdminOverviewResponse getOverview();

}
