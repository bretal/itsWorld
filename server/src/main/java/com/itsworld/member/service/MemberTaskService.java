package com.itsworld.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.member.dto.MemberAwardResult;
import com.itsworld.member.dto.MemberTaskRequest;
import com.itsworld.member.dto.MemberTaskView;
import com.itsworld.member.entity.MemberTask;
import com.itsworld.member.entity.MemberTaskRecord;
import com.itsworld.member.mapper.MemberTaskMapper;
import com.itsworld.member.mapper.MemberTaskRecordMapper;
import com.itsworld.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 会员任务业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTaskService {

    private static final String TASK_CHECK_IN = "DAILY_CHECKIN";
    private static final Set<String> PERIOD_TYPES = Set.of("DAILY", "WEEKLY", "ONCE");

    private final MemberTaskMapper memberTaskMapper;
    private final MemberTaskRecordMapper memberTaskRecordMapper;
    private final MemberService memberService;

    public List<MemberTaskView> listTasks(Long userId) {
        List<MemberTask> tasks = memberTaskMapper.selectList(new LambdaQueryWrapper<MemberTask>()
                .eq(MemberTask::getStatus, 1)
                .orderByAsc(MemberTask::getSortOrder)
                .orderByAsc(MemberTask::getId));

        List<MemberTaskView> views = new ArrayList<>();
        for (MemberTask task : tasks) {
            MemberTaskRecord record = findRecord(userId, task);
            int progress = record != null && record.getProgress() != null ? record.getProgress() : 0;
            int rewardStatus = record != null && record.getRewardStatus() != null ? record.getRewardStatus() : 0;
            boolean claimable = progress >= task.getTargetValue() && rewardStatus == 0;
            views.add(MemberTaskView.builder()
                    .id(task.getId())
                    .taskCode(task.getTaskCode())
                    .taskName(task.getTaskName())
                    .description(task.getDescription())
                    .periodType(task.getPeriodType())
                    .targetValue(task.getTargetValue())
                    .growthReward(task.getGrowthReward())
                    .progress(progress)
                    .rewardStatus(rewardStatus)
                    .claimable(claimable)
                    .build());
        }
        return views;
    }

    @Transactional
    public void checkIn(Long userId) {
        recordProgress(userId, TASK_CHECK_IN);
        log.info("会员签到: userId={}", userId);
    }

    @Transactional
    public MemberAwardResult claim(Long userId, Long taskId) {
        MemberTask task = memberTaskMapper.selectById(taskId);
        if (task == null || !Integer.valueOf(1).equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_TASK_NOT_FOUND);
        }
        MemberTaskRecord record = findRecord(userId, task);
        if (record == null || record.getProgress() == null
                || record.getProgress() < task.getTargetValue()) {
            throw new BusinessException(ErrorCode.MEMBER_TASK_NOT_COMPLETED);
        }
        if (Integer.valueOf(1).equals(record.getRewardStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_TASK_ALREADY_CLAIMED);
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = memberTaskRecordMapper.update(null, new LambdaUpdateWrapper<MemberTaskRecord>()
                .eq(MemberTaskRecord::getId, record.getId())
                .eq(MemberTaskRecord::getRewardStatus, 0)
                .set(MemberTaskRecord::getRewardStatus, 1)
                .set(MemberTaskRecord::getClaimedAt, now)
                .set(MemberTaskRecord::getUpdatedAt, now));
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.MEMBER_TASK_ALREADY_CLAIMED);
        }

        int growth = task.getGrowthReward() != null ? task.getGrowthReward() : 0;
        MemberAwardResult result = memberService.award(userId, growth,
                "TASK", "TASK_RECORD:" + record.getId(), "任务奖励：" + task.getTaskName());

        log.info("任务奖励领取: userId={}, taskId={}, growth={}",
                userId, taskId, growth);
        return result;
    }

    @Transactional
    public void recordProgress(Long userId, String taskCode) {
        if (taskCode == null || taskCode.isBlank()) {
            return;
        }
        MemberTask task = memberTaskMapper.selectOne(new LambdaQueryWrapper<MemberTask>()
                .eq(MemberTask::getTaskCode, taskCode)
                .eq(MemberTask::getStatus, 1)
                .last("LIMIT 1"));
        if (task == null) {
            return;
        }
        memberService.getOrCreateAccount(userId);
        memberTaskRecordMapper.upsertProgress(userId, task.getId(), periodKey(task), task.getTargetValue());
        log.debug("任务进度更新: userId={}, taskCode={}", userId, taskCode);
    }

    public List<MemberTask> getAdminTasks() {
        return memberTaskMapper.selectList(new LambdaQueryWrapper<MemberTask>()
                .orderByAsc(MemberTask::getSortOrder)
                .orderByAsc(MemberTask::getId));
    }

    @Transactional
    public MemberTask createTask(MemberTaskRequest request) {
        validatePeriodType(request.getPeriodType());
        validateTaskCodeUnique(request.getTaskCode(), null);
        MemberTask task = MemberTask.builder()
                .taskCode(request.getTaskCode().trim())
                .taskName(request.getTaskName().trim())
                .description(request.getDescription())
                .periodType(request.getPeriodType())
                .targetValue(request.getTargetValue())
                .growthReward(request.getGrowthReward() != null ? request.getGrowthReward() : 0)
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        memberTaskMapper.insert(task);
        log.info("会员任务创建: taskId={}, code={}", task.getId(), task.getTaskCode());
        return task;
    }

    @Transactional
    public MemberTask updateTask(Long taskId, MemberTaskRequest request) {
        MemberTask task = memberTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.MEMBER_TASK_NOT_FOUND);
        }
        validatePeriodType(request.getPeriodType());
        validateTaskCodeUnique(request.getTaskCode(), taskId);

        task.setTaskCode(request.getTaskCode().trim());
        task.setTaskName(request.getTaskName().trim());
        task.setDescription(request.getDescription());
        task.setPeriodType(request.getPeriodType());
        task.setTargetValue(request.getTargetValue());
        task.setGrowthReward(request.getGrowthReward() != null ? request.getGrowthReward() : task.getGrowthReward());
        task.setStatus(request.getStatus() != null ? request.getStatus() : task.getStatus());
        task.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : task.getSortOrder());
        task.setUpdatedAt(LocalDateTime.now());
        memberTaskMapper.updateById(task);
        log.info("会员任务更新: taskId={}, code={}", taskId, task.getTaskCode());
        return task;
    }

    @Transactional
    public void disableTask(Long taskId) {
        MemberTask task = memberTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.MEMBER_TASK_NOT_FOUND);
        }
        task.setStatus(0);
        task.setUpdatedAt(LocalDateTime.now());
        memberTaskMapper.updateById(task);
        log.info("会员任务下架: taskId={}", taskId);
    }

    private MemberTaskRecord findRecord(Long userId, MemberTask task) {
        return memberTaskRecordMapper.selectOne(new LambdaQueryWrapper<MemberTaskRecord>()
                .eq(MemberTaskRecord::getUserId, userId)
                .eq(MemberTaskRecord::getTaskId, task.getId())
                .eq(MemberTaskRecord::getPeriodKey, periodKey(task))
                .last("LIMIT 1"));
    }

    private String periodKey(MemberTask task) {
        LocalDate today = LocalDate.now();
        String periodType = task.getPeriodType();
        if ("DAILY".equals(periodType)) {
            return String.format("%04d%02d%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        }
        if ("WEEKLY".equals(periodType)) {
            int weekYear = today.get(WeekFields.ISO.weekBasedYear());
            int week = today.get(WeekFields.ISO.weekOfWeekBasedYear());
            return weekYear + "-W" + String.format("%02d", week);
        }
        return "ONCE";
    }

    private void validatePeriodType(String periodType) {
        if (periodType == null || !PERIOD_TYPES.contains(periodType)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "任务周期仅支持 DAILY/WEEKLY/ONCE");
        }
    }

    private void validateTaskCodeUnique(String taskCode, Long excludeTaskId) {
        LambdaQueryWrapper<MemberTask> wrapper = new LambdaQueryWrapper<MemberTask>()
                .eq(MemberTask::getTaskCode, taskCode.trim());
        if (excludeTaskId != null) {
            wrapper.ne(MemberTask::getId, excludeTaskId);
        }
        Long count = memberTaskMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.MEMBER_TASK_CODE_EXISTS);
        }
    }

}
