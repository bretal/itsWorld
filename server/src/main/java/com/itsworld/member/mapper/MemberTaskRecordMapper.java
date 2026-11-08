package com.itsworld.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.member.entity.MemberTaskRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会员任务进度数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface MemberTaskRecordMapper extends BaseMapper<MemberTaskRecord> {

    /**
     * 原子递增任务进度（不存在则创建，已达目标则保持不变）
     *
     * @param userId      用户ID
     * @param taskId      任务ID
     * @param periodKey   周期键
     * @param targetValue 目标次数
     * @return 影响行数
     */
    @Insert("INSERT INTO member_task_record "
            + "(user_id, task_id, period_key, progress, reward_status, created_at, updated_at) "
            + "VALUES (#{userId}, #{taskId}, #{periodKey}, 1, 0, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE progress = LEAST(progress + 1, #{targetValue}), updated_at = NOW()")
    int upsertProgress(@Param("userId") Long userId,
                       @Param("taskId") Long taskId,
                       @Param("periodKey") String periodKey,
                       @Param("targetValue") Integer targetValue);

}
