package com.itsworld.admin.service.impl;

import com.itsworld.admin.dto.AdminOverviewResponse;
import com.itsworld.admin.mapper.AdminOverviewMapper;
import com.itsworld.admin.service.AdminOverviewService;
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
public class AdminOverviewServiceImpl implements AdminOverviewService {

    private final AdminOverviewMapper adminOverviewMapper;

    @Override
    public AdminOverviewResponse getOverview() {
        return adminOverviewMapper.selectOverview();
    }

}
