package com.itsworld.admin.service;

import com.itsworld.admin.dto.AdminOverviewResponse;
import com.itsworld.admin.mapper.AdminOverviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理后台总览业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Service
@RequiredArgsConstructor
public class AdminOverviewService {

    private final AdminOverviewMapper adminOverviewMapper;

    public AdminOverviewResponse getOverview() {
        return adminOverviewMapper.selectOverview();
    }

}
