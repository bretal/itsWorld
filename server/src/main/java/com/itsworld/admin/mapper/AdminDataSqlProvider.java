package com.itsworld.admin.mapper;

import com.itsworld.common.exception.BusinessException;
import java.util.Map;

/** SQL 标识符仅来自固定资源白名单，所有请求值均使用 MyBatis 参数绑定。 */
public class AdminDataSqlProvider {
    public record Resource(String table, String owner, String search, boolean softDelete) { }
    private static final Map<String, Resource> RESOURCES = Map.of(
        "pets", new Resource("pet_profile", "user_id", "pet_name", true),
        "diaries", new Resource("pet_diary", "user_id", "content", false),
        "posts", new Resource("community_post", "user_id", "content", true),
        "comments", new Resource("community_comment", "user_id", "content", true),
        "weight", new Resource("pet_weight_record", null, null, false),
        "feeding", new Resource("pet_feeding_record", null, "notes", false),
        "vaccine", new Resource("pet_vaccine_record", null, "vaccine_name", false),
        "checkup", new Resource("pet_health_checkup", null, "vet_clinic", false));


    private static String from(Map<String, Object> params) {
        String kind = (String) params.get("kind");
        Resource resource = resource(kind);
        String owner = resource.owner() == null ? "p.user_id" : "r." + resource.owner();
        String join = resource.owner() == null
            ? " LEFT JOIN pet_profile p ON p.id=r.pet_id LEFT JOIN sys_user u ON u.id=p.user_id"
            : " LEFT JOIN sys_user u ON u.id=r.user_id";
        String where = resource.softDelete() ? " WHERE r.deleted_at IS NULL" : " WHERE 1=1";
        if (params.get("recordId") != null) { where += " AND r.id=#{recordId}"; }
        if (params.get("userId") != null) { where += " AND " + owner + "=#{userId}"; }
        if (params.get("petId") != null) {
            if ("comments".equals(kind)) { throw new BusinessException("评论请按帖子筛选"); }
            where += " AND r." + ("pets".equals(kind) ? "id" : "pet_id") + "=#{petId}";
        }
        if (params.get("postId") != null && "comments".equals(kind)) { where += " AND r.post_id=#{postId}"; }
        if (params.get("keyword") != null) {
            where += " AND (u.username LIKE #{keyword}"
                + (resource.search() == null ? "" : " OR r." + resource.search() + " LIKE #{keyword}") + ")";
        }
        return " FROM " + resource.table() + " r" + join + where;
    }
    public static String count(Map<String, Object> params) { return "SELECT COUNT(*)" + from(params); }
    public static String list(Map<String, Object> params) {
        Resource resource = resource((String) params.get("kind"));
        String owner = resource.owner() == null ? "p.user_id" : "r." + resource.owner();
        return "SELECT r.*,u.username AS owner_name," + owner + " AS owner_id" + from(params)
            + " ORDER BY r.id DESC LIMIT #{limit} OFFSET #{offset}";
    }
    public static String detail(Map<String, Object> params) {
        Resource resource = resource((String) params.get("kind"));
        return "SELECT * FROM " + resource.table() + " WHERE id=#{id}"
            + (resource.softDelete() ? " AND deleted_at IS NULL" : "");
    }
    private static Resource resource(String kind) {
        Resource resource = RESOURCES.get(kind);
        if (resource == null) { throw new BusinessException("不支持的管理资源"); }
        return resource;
    }
}
