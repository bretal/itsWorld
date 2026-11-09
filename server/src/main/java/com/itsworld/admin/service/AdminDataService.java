package com.itsworld.admin.service;

import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 跨用户管理查询。表与列只来自服务端白名单，所有筛选值使用绑定参数。 */
@Service
@RequiredArgsConstructor
public class AdminDataService {
    private final JdbcTemplate jdbc;
    private record Resource(String table, String owner, String search, boolean softDelete) { }
    private static final Map<String, Resource> RESOURCES = Map.of(
        "pets", new Resource("pet_profile", "user_id", "pet_name", true),
        "diaries", new Resource("pet_diary", "user_id", "content", false),
        "posts", new Resource("community_post", "user_id", "content", true),
        "comments", new Resource("community_comment", "user_id", "content", true),
        "weight", new Resource("pet_weight_record", null, null, false),
        "feeding", new Resource("pet_feeding_record", null, "notes", false),
        "vaccine", new Resource("pet_vaccine_record", null, "vaccine_name", false),
        "checkup", new Resource("pet_health_checkup", null, "vet_clinic", false));

    public PageResult<Map<String, Object>> list(String kind, Long userId, Long petId, Long postId,
                                               String keyword, Long recordId, int page, int size) {
        Resource resource = resource(kind);
        int limit = Math.min(Math.max(size, 1), 100);
        int current = Math.max(page, 1);
        List<Object> args = new ArrayList<>();
        String owner = resource.owner() == null ? "p.user_id" : "r." + resource.owner();
        String join = resource.owner() == null
            ? " LEFT JOIN pet_profile p ON p.id=r.pet_id LEFT JOIN sys_user u ON u.id=p.user_id"
            : " LEFT JOIN sys_user u ON u.id=r.user_id";
        String where = resource.softDelete() ? " WHERE r.deleted_at IS NULL" : " WHERE 1=1";
        if (recordId != null) { where += " AND r.id=?"; args.add(recordId); }
        if (userId != null) { where += " AND " + owner + "=?"; args.add(userId); }
        if (petId != null) {
            where += " AND r." + ("pets".equals(kind) ? "id" : "pet_id") + "=?";
            if ("comments".equals(kind)) { throw new BusinessException("评论请按帖子筛选"); }
            args.add(petId);
        }
        if (postId != null && "comments".equals(kind)) { where += " AND r.post_id=?"; args.add(postId); }
        if (keyword != null && !keyword.isBlank()) {
            where += " AND (u.username LIKE ?" + (resource.search() == null ? "" : " OR r." + resource.search() + " LIKE ?") + ")";
            args.add("%" + keyword.trim() + "%");
            if (resource.search() != null) { args.add("%" + keyword.trim() + "%"); }
        }
        String from = " FROM " + resource.table() + " r" + join + where;
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + from, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args); pageArgs.add(limit); pageArgs.add((long)(current - 1) * limit);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT r.*,u.username AS owner_name," + owner + " AS owner_id" + from + " ORDER BY r.id DESC LIMIT ? OFFSET ?", pageArgs.toArray());
        return PageResult.of(rows, total == null ? 0 : total, current, limit);
    }

    public Map<String, Object> detail(String kind, long id) {
        Resource resource = resource(kind);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + resource.table() + " WHERE id=?" + (resource.softDelete() ? " AND deleted_at IS NULL" : ""), id);
        if (rows.isEmpty()) { throw new BusinessException("记录不存在"); }
        return rows.get(0);
    }

    /** 下架不删除原内容；评论软删除并同步可见评论数。 */
    @Transactional
    public void moderate(String kind, long id, String action, String reason, long adminId) {
        Map<String, Object> row = detail(kind, id);
        String sql;
        if ("posts".equals(kind) && ("hide".equals(action) || "publish".equals(action))) {
            sql = "UPDATE community_post SET status=? WHERE id=?";
            jdbc.update(sql, "publish".equals(action) ? 1 : 0, id);
        } else if ("diaries".equals(kind) && "hide".equals(action)) {
            jdbc.update("UPDATE pet_diary SET is_published=0 WHERE id=?", id);
        } else if ("comments".equals(kind) && "remove".equals(action)) {
            jdbc.update("UPDATE community_comment SET deleted_at=NOW() WHERE id=? AND deleted_at IS NULL", id);
            jdbc.update("UPDATE community_post SET comment_count=(SELECT COUNT(*) FROM community_comment WHERE post_id=? AND deleted_at IS NULL) WHERE id=?", row.get("post_id"), row.get("post_id"));
        } else { throw new BusinessException("不支持的管理操作"); }
        jdbc.update("INSERT INTO admin_moderation_log(target_type,target_id,action,reason,admin_id) VALUES(?,?,?,?,?)", kind,id,action,reason,adminId);
    }

    private Resource resource(String kind) {
        Resource resource = RESOURCES.get(kind);
        if (resource == null) { throw new BusinessException("不支持的管理资源"); }
        return resource;
    }
}
