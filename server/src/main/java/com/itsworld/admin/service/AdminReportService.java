package com.itsworld.admin.service;

import com.itsworld.admin.mapper.AdminReportMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminReportService {
    private final AdminReportMapper mapper;
    private final AdminDataService data;

    public PageResult<Map<String, Object>> reports(String status, int page, int size) {
        int current = Math.max(1, page), limit = Math.min(100, Math.max(1, size));
        String filter = status == null || status.isBlank() ? null : status;
        long total = mapper.count(filter);
        return PageResult.of(mapper.list(filter, limit, (long) (current - 1) * limit), total, current, limit);
    }

    @Transactional
    public void create(String targetType, long targetId, Long reporterId, String reason) {
        data.detail(targetType, targetId);
        mapper.insert(targetType, targetId, reporterId, reason);
    }

    @Transactional
    public void resolve(long id, String status, String resolution, long adminId) {
        if (mapper.resolve(id, status, resolution, adminId) != 1) {
            throw new BusinessException("举报不存在或已经处理");
        }
    }

    public List<Map<String, Object>> logs(String targetType, long targetId) {
        return mapper.logs(targetType, targetId);
    }
}
