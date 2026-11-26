package com.itsworld.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.community.dto.FollowUserResponse;
import com.itsworld.community.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 关注关系数据访问接口
 *
 * @author Claude
 * @date 2026-10-30
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    @Select("""
            SELECT u.id, u.nick_name, u.avatar_url, u.bio,
                   (outgoing.id IS NOT NULL) AS is_following,
                   (incoming.id IS NOT NULL) AS follows_me
            FROM sys_user u
            LEFT JOIN user_follow outgoing
              ON outgoing.follower_id = #{viewerId} AND outgoing.following_id = u.id
            LEFT JOIN user_follow incoming
              ON incoming.follower_id = u.id AND incoming.following_id = #{viewerId}
            WHERE u.deleted_at IS NULL AND u.status = 1 AND u.id != #{viewerId}
              AND LOCATE(#{keyword}, u.nick_name) > 0
            ORDER BY (u.nick_name = #{keyword}) DESC, LOCATE(#{keyword}, u.nick_name), u.id
            """)
    Page<FollowUserResponse> searchFollowUsers(Page<FollowUserResponse> page,
                                               @Param("viewerId") Long viewerId,
                                               @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT u.id, COALESCE(NULLIF(u.nick_name, ''), u.username) AS nick_name,
                   u.avatar_url, u.bio,
            <choose>
              <when test="fans">
                (reverse_follow.id IS NOT NULL) AS is_following, 1 AS follows_me
                FROM user_follow f
                JOIN sys_user u ON u.id = f.follower_id AND u.deleted_at IS NULL
                LEFT JOIN user_follow reverse_follow
                  ON reverse_follow.follower_id = f.following_id
                 AND reverse_follow.following_id = f.follower_id
                WHERE f.following_id = #{userId}
              </when>
              <otherwise>
                1 AS is_following, (reverse_follow.id IS NOT NULL) AS follows_me
                FROM user_follow f
                JOIN sys_user u ON u.id = f.following_id AND u.deleted_at IS NULL
                LEFT JOIN user_follow reverse_follow
                  ON reverse_follow.follower_id = f.following_id
                 AND reverse_follow.following_id = f.follower_id
                WHERE f.follower_id = #{userId}
              </otherwise>
            </choose>
            ORDER BY f.created_at DESC, f.id DESC
            </script>
            """)
    Page<FollowUserResponse> selectFollowUsers(Page<FollowUserResponse> page,
                                               @Param("userId") Long userId,
                                               @Param("fans") boolean fans);

    /**
     * 获取用户的粉丝列表
     *
     * @param userId 用户ID
     * @return 粉丝列表
     */
    @Select("SELECT * FROM user_follow WHERE following_id = #{userId} ORDER BY created_at DESC")
    List<Follow> selectFollowers(Long userId);

    /**
     * 获取用户关注的人列表
     *
     * @param userId 用户ID
     * @return 关注列表
     */
    @Select("SELECT * FROM user_follow WHERE follower_id = #{userId} ORDER BY created_at DESC")
    List<Follow> selectFollowing(Long userId);

    /**
     * 检查用户A是否关注了用户B
     *
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 是否关注
     */
    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    Integer checkFollowing(Long followerId, Long followingId);

    /**
     * 获取粉丝数量
     *
     * @param userId 用户ID
     * @return 粉丝数量
     */
    @Select("""
            SELECT COUNT(*) FROM user_follow f
            JOIN sys_user u ON u.id = f.follower_id AND u.deleted_at IS NULL
            WHERE f.following_id = #{userId}
            """)
    Integer getFollowerCount(Long userId);

    /**
     * 获取关注数量
     *
     * @param userId 用户ID
     * @return 关注数量
     */
    @Select("""
            SELECT COUNT(*) FROM user_follow f
            JOIN sys_user u ON u.id = f.following_id AND u.deleted_at IS NULL
            WHERE f.follower_id = #{userId}
            """)
    Integer getFollowingCount(Long userId);

}
