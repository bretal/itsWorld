package com.itsworld.admin.service;

import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import com.itsworld.admin.mapper.AdminDataMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** 跨用户管理查询。表与列只来自服务端白名单，所有筛选值使用绑定参数。 */
@Service
@RequiredArgsConstructor
public class AdminDataService {
    private final AdminDataMapper mapper;
    private static final Set<String> RESOURCE_KINDS = Set.of(
        "pets", "diaries", "posts", "comments", "weight", "feeding", "vaccine", "checkup");

    private void validateKind(String kind) {
        if (kind == null || !RESOURCE_KINDS.contains(kind)) {
            throw new BusinessException("不支持的管理资源");
        }
    }

    public PageResult<Map<String, Object>> list(String kind, Long userId, Long petId, Long postId,
                                               String keyword, Long recordId, int page, int size) {
        validateKind(kind);
        if (petId != null && "comments".equals(kind)) {
            throw new BusinessException("评论请按帖子筛选");
        }
        int limit = Math.min(Math.max(size, 1), 100);
        int current = Math.max(page, 1);
        Map<String, Object> query = new HashMap<>();
        query.put("kind", kind);
        query.put("userId", userId);
        query.put("petId", petId);
        query.put("postId", postId);
        query.put("recordId", recordId);
        query.put("keyword", keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%");
        query.put("limit", limit);
        query.put("offset", (long) (current - 1) * limit);
        long total = mapper.count(query);
        return PageResult.of(mapper.list(query), total, current, limit);
    }

    public Map<String, Object> detail(String kind, long id) {
        validateKind(kind);
        Map<String, Object> row = mapper.detail(kind, id);
        if (row == null) { throw new BusinessException("记录不存在"); }
        return row;
    }

    /** 下架不删除原内容；评论软删除并同步可见评论数。 */
    @Transactional
    public void moderate(String kind, long id, String action, String reason, long adminId) {
        Map<String, Object> row = detail(kind, id);
        if ("posts".equals(kind) && ("hide".equals(action) || "publish".equals(action))) {
            mapper.updatePostStatus(id, "publish".equals(action) ? 1 : 0);
        } else if ("diaries".equals(kind) && "hide".equals(action)) {
            mapper.hideDiary(id);
        } else if ("comments".equals(kind) && "remove".equals(action)) {
            mapper.removeComment(id);
            mapper.refreshCommentCount(row.get("post_id"));
        } else { throw new BusinessException("不支持的管理操作"); }
        mapper.insertModerationLog(kind, id, action, reason, adminId);
    }

}
