package com.itsworld.member.controller;

import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.member.dto.CouponView;
import com.itsworld.member.dto.GiftView;
import com.itsworld.member.dto.MemberAwardResult;
import com.itsworld.member.dto.MemberLevelView;
import com.itsworld.member.dto.MemberOverviewResponse;
import com.itsworld.member.dto.MemberTaskView;
import com.itsworld.member.entity.MemberGrowthLog;
import com.itsworld.member.entity.MemberLevelHistory;
import com.itsworld.member.service.CouponService;
import com.itsworld.member.service.GiftService;
import com.itsworld.member.service.MemberService;
import com.itsworld.member.service.MemberTaskService;
import com.itsworld.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员中心控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberTaskService memberTaskService;
    private final CouponService couponService;
    private final GiftService giftService;
    private final CurrentUserService currentUserService;

    /**
     * 会员中心总览
     */
    @GetMapping("/overview")
    public Result<MemberOverviewResponse> getOverview() {
        Long userId = currentUserService.getUserId();
        return Result.success(memberService.getOverview(userId));
    }

    /**
     * 等级与权益列表
     */
    @GetMapping("/levels")
    public Result<List<MemberLevelView>> getLevels() {
        Long userId = currentUserService.getUserId();
        return Result.success(memberService.getLevels(userId));
    }

    /**
     * 成长值流水
     */
    @GetMapping("/growth-logs")
    public Result<PageResult<MemberGrowthLog>> getGrowthLogs(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        Long userId = currentUserService.getUserId();
        return Result.success(memberService.getGrowthLogs(userId,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }


    /**
     * 等级变更历史
     */
    @GetMapping("/level-history")
    public Result<List<MemberLevelHistory>> getLevelHistory() {
        Long userId = currentUserService.getUserId();
        return Result.success(memberService.getLevelHistory(userId));
    }

    /**
     * 当前任务列表
     */
    @GetMapping("/tasks")
    public Result<List<MemberTaskView>> getTasks() {
        Long userId = currentUserService.getUserId();
        return Result.success(memberTaskService.listTasks(userId));
    }

    /**
     * 每日签到
     */
    @PostMapping("/tasks/check-in")
    public Result<Void> checkIn() {
        Long userId = currentUserService.getUserId();
        memberTaskService.checkIn(userId);
        return Result.success(null, "签到成功");
    }

    /**
     * 领取任务奖励
     */
    @PostMapping("/tasks/{taskId}/claim")
    public Result<MemberAwardResult> claimTask(@PathVariable Long taskId) {
        Long userId = currentUserService.getUserId();
        return Result.success(memberTaskService.claim(userId, taskId), "领取成功");
    }

    /**
     * 我的优惠券
     */
    @GetMapping("/coupons")
    public Result<List<CouponView>> getCoupons(@RequestParam(required = false) String status) {
        Long userId = currentUserService.getUserId();
        return Result.success(couponService.listUserCoupons(userId, status));
    }

    /**
     * 礼包列表
     */
    @GetMapping("/gifts")
    public Result<List<GiftView>> getGifts() {
        Long userId = currentUserService.getUserId();
        return Result.success(giftService.listGifts(userId));
    }

    /**
     * 领取礼包
     */
    @PostMapping("/gifts/{giftId}/claim")
    public Result<Void> claimGift(@PathVariable Long giftId) {
        Long userId = currentUserService.getUserId();
        giftService.claimGift(userId, giftId);
        return Result.success(null, "领取成功");
    }

}
