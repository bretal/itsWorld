package com.itsworld.admin.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminDataMapper {
    @SelectProvider(type=AdminDataSqlProvider.class, method="count")
    long count(Map<String, Object> query);
    @SelectProvider(type=AdminDataSqlProvider.class, method="list")
    List<Map<String, Object>> list(Map<String, Object> query);
    @SelectProvider(type=AdminDataSqlProvider.class, method="detail")
    Map<String, Object> detail(@Param("kind") String kind, @Param("id") long id);
    @Update("UPDATE community_post SET status=#{status} WHERE id=#{id}")
    int updatePostStatus(@Param("id") long id, @Param("status") int status);
    @Update("UPDATE pet_diary SET is_published=0 WHERE id=#{id}")
    int hideDiary(long id);
    @Update("UPDATE community_comment SET deleted_at=NOW() WHERE id=#{id} AND deleted_at IS NULL")
    int removeComment(long id);
    @Update("UPDATE community_post SET comment_count=(SELECT COUNT(*) FROM community_comment WHERE post_id=#{postId} AND deleted_at IS NULL) WHERE id=#{postId}")
    int refreshCommentCount(@Param("postId") Object postId);
    @Insert("INSERT INTO admin_moderation_log(target_type,target_id,action,reason,admin_id) VALUES(#{kind},#{id},#{action},#{reason},#{adminId})")
    int insertModerationLog(@Param("kind") String kind, @Param("id") long id,
        @Param("action") String action, @Param("reason") String reason, @Param("adminId") long adminId);
}
