package com.itsworld.admin.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminReportMapper {
    @Select("<script>SELECT COUNT(*) FROM content_report <if test='status != null'>WHERE status=#{status}</if></script>")
    long count(@Param("status") String status);
    @Select("<script>SELECT * FROM content_report <if test='status != null'>WHERE status=#{status}</if> ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<Map<String, Object>> list(@Param("status") String status, @Param("limit") int limit, @Param("offset") long offset);
    @Insert("INSERT INTO content_report(target_type,target_id,reporter_id,reason) VALUES(#{targetType},#{targetId},#{reporterId},#{reason})")
    int insert(@Param("targetType") String targetType, @Param("targetId") long targetId,
        @Param("reporterId") Long reporterId, @Param("reason") String reason);
    @Update("UPDATE content_report SET status=#{status},resolution=#{resolution},handled_by=#{adminId},handled_at=NOW() WHERE id=#{id} AND status='PENDING'")
    int resolve(@Param("id") long id, @Param("status") String status,
        @Param("resolution") String resolution, @Param("adminId") long adminId);
    @Select("SELECT * FROM admin_moderation_log WHERE target_type=#{targetType} AND target_id=#{targetId} ORDER BY id DESC LIMIT 100")
    List<Map<String, Object>> logs(@Param("targetType") String targetType, @Param("targetId") long targetId);
}
