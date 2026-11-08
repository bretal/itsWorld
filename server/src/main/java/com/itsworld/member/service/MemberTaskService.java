package com.itsworld.member.service;

import com.itsworld.member.dto.MemberAwardResult;
import com.itsworld.member.dto.MemberTaskRequest;
import com.itsworld.member.dto.MemberTaskView;
import com.itsworld.member.entity.MemberTask;

import java.util.List;

/**
 * 会员任务业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface MemberTaskService {

    /**
     * 当前用户任务列表（含本期进度）
     *
     * @param userId 用户ID
     * @return 任务列表
     */
    List<MemberTaskView> listTasks(Long userId);

    /**
     * 每日签到
     *
     * @param userId 用户ID
     */
    void checkIn(Long userId);

    /**
     * 领取任务奖励
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 发放结果
     */
    MemberAwardResult claim(Long userId, Long taskId);

    /**
     * 记录任务进度（由业务埋点调用）
     *
     * @param userId   用户ID
     * @param taskCode 任务编码
     */
    void recordProgress(Long userId, String taskCode);

    /**
     * 管理端任务列表
     *
     * @return 任务列表
     */
    List<MemberTask> getAdminTasks();

    /**
     * 新建任务
     *
     * @param request 任务配置
     * @return 任务
     */
    MemberTask createTask(MemberTaskRequest request);

    /**
     * 更新任务
     *
     * @param taskId  任务ID
     * @param request 任务配置
     * @return 任务
     */
    MemberTask updateTask(Long taskId, MemberTaskRequest request);

    /**
     * 下架任务
     *
     * @param taskId 任务ID
     */
    void disableTask(Long taskId);

}
