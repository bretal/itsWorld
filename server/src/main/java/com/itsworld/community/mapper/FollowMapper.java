package com.itsworld.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.community.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
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
    @Select("SELECT COUNT(*) FROM user_follow WHERE following_id = #{userId}")
    Integer getFollowerCount(Long userId);

    /**
     * 获取关注数量
     *
     * @param userId 用户ID
     * @return 关注数量
     */
    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{userId}")
    Integer getFollowingCount(Long userId);

}
