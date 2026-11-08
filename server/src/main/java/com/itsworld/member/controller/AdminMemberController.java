package com.itsworld.member.controller;

import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.member.dto.AdminMemberAccountView;
import com.itsworld.member.dto.CouponIssueRequest;
import com.itsworld.member.dto.CouponTemplateRequest;
import com.itsworld.member.dto.CouponView;
import com.itsworld.member.dto.MemberGiftRequest;
import com.itsworld.member.dto.MemberLevelRequest;
import com.itsworld.member.dto.MemberLevelView;
import com.itsworld.member.dto.MemberTaskRequest;
import com.itsworld.member.dto.UserStatusRequest;
import com.itsworld.member.entity.CouponTemplate;
import com.itsworld.member.entity.MemberGift;
import com.itsworld.member.entity.MemberGrowthLog;
import com.itsworld.member.entity.MemberTask;
import com.itsworld.member.service.CouponService;
import com.itsworld.member.service.GiftService;
import com.itsworld.member.service.MemberService;
import com.itsworld.member.service.MemberTaskService;
import com.itsworld.security.service.CurrentUserService;
import com.itsworld.wallet.dto.RechargeOrderView;
import com.itsworld.wallet.dto.WalletTransactionView;
import com.itsworld.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端会员运营控制器（仅 ROLE_ADMIN）
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/member")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;
    private final MemberTaskService memberTaskService;
    private final CouponService couponService;
    private final GiftService giftService;
    private final WalletService walletService;
    private final CurrentUserService currentUserService;

    /**
     * 会员账户分页（全量用户视图）
     */
    @GetMapping("/accounts")
    public Result<PageResult<AdminMemberAccountView>> getAccounts(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String role) {
        return Result.success(memberService.getAdminAccounts(
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size),
                keyword, status, role));
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/accounts/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId,
                                         @Valid @RequestBody UserStatusRequest request) {
        Long operatorId = currentUserService.getUserId();
        memberService.updateUserStatus(operatorId, userId, request.getStatus());
        return Result.success(null, "状态已更新");
    }

    /**
     * 指定用户的成长值流水
     */
    @GetMapping("/accounts/{userId}/growth-logs")
    public Result<PageResult<MemberGrowthLog>> getGrowthLogs(
            @PathVariable Long userId,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(memberService.getGrowthLogs(userId,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }


    /**
     * 指定用户的钱包流水
     */
    @GetMapping("/accounts/{userId}/wallet-transactions")
    public Result<PageResult<WalletTransactionView>> getWalletTransactions(
            @PathVariable Long userId,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(walletService.getUserTransactions(userId,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    /**
     * 指定用户的充值单
     */
    @GetMapping("/accounts/{userId}/recharges")
    public Result<PageResult<RechargeOrderView>> getRecharges(
            @PathVariable Long userId,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(walletService.getUserRecharges(userId,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    /**
     * 指定用户的优惠券
     */
    @GetMapping("/accounts/{userId}/coupons")
    public Result<List<CouponView>> getCoupons(@PathVariable Long userId,
                                               @RequestParam(required = false) String status) {
        return Result.success(couponService.listUserCoupons(userId, status));
    }

    /**
     * 等级列表（含停用）
     */
    @GetMapping("/levels")
    public Result<List<MemberLevelView>> getLevels() {
        return Result.success(memberService.getAdminLevels());
    }

    /**
     * 新建等级
     */
    @PostMapping("/levels")
    public Result<MemberLevelView> createLevel(@Valid @RequestBody MemberLevelRequest request) {
        return Result.success(memberService.createLevel(request), "等级已创建");
    }

    /**
     * 更新等级
     */
    @PutMapping("/levels/{levelId}")
    public Result<MemberLevelView> updateLevel(@PathVariable Long levelId,
                                               @Valid @RequestBody MemberLevelRequest request) {
        return Result.success(memberService.updateLevel(levelId, request), "等级已更新");
    }

    /**
     * 下架等级
     */
    @DeleteMapping("/levels/{levelId}")
    public Result<Void> disableLevel(@PathVariable Long levelId) {
        memberService.disableLevel(levelId);
        return Result.success(null, "等级已下架");
    }

    /**
     * 任务列表
     */
    @GetMapping("/tasks")
    public Result<List<MemberTask>> getTasks() {
        return Result.success(memberTaskService.getAdminTasks());
    }

    /**
     * 新建任务
     */
    @PostMapping("/tasks")
    public Result<MemberTask> createTask(@Valid @RequestBody MemberTaskRequest request) {
        return Result.success(memberTaskService.createTask(request), "任务已创建");
    }

    /**
     * 更新任务
     */
    @PutMapping("/tasks/{taskId}")
    public Result<MemberTask> updateTask(@PathVariable Long taskId,
                                         @Valid @RequestBody MemberTaskRequest request) {
        return Result.success(memberTaskService.updateTask(taskId, request), "任务已更新");
    }

    /**
     * 下架任务
     */
    @DeleteMapping("/tasks/{taskId}")
    public Result<Void> disableTask(@PathVariable Long taskId) {
        memberTaskService.disableTask(taskId);
        return Result.success(null, "任务已下架");
    }

    /**
     * 优惠券模板列表
     */
    @GetMapping("/coupon-templates")
    public Result<List<CouponTemplate>> getCouponTemplates() {
        return Result.success(couponService.getAdminTemplates());
    }

    /**
     * 新建优惠券模板
     */
    @PostMapping("/coupon-templates")
    public Result<CouponTemplate> createCouponTemplate(
            @Valid @RequestBody CouponTemplateRequest request) {
        return Result.success(couponService.createTemplate(request), "券模板已创建");
    }

    /**
     * 更新优惠券模板
     */
    @PutMapping("/coupon-templates/{templateId}")
    public Result<CouponTemplate> updateCouponTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody CouponTemplateRequest request) {
        return Result.success(couponService.updateTemplate(templateId, request), "券模板已更新");
    }

    /**
     * 下架优惠券模板
     */
    @DeleteMapping("/coupon-templates/{templateId}")
    public Result<Void> disableCouponTemplate(@PathVariable Long templateId) {
        couponService.disableTemplate(templateId);
        return Result.success(null, "券模板已下架");
    }

    /**
     * 发券
     */
    @PostMapping("/coupons/issue")
    public Result<Void> issueCoupons(@Valid @RequestBody CouponIssueRequest request) {
        couponService.issueCoupons(request.getUserId(), request.getTemplateId(), request.getQuantity());
        return Result.success(null, "发券成功");
    }

    /**
     * 礼包列表
     */
    @GetMapping("/gifts")
    public Result<List<MemberGift>> getGifts() {
        return Result.success(giftService.getAdminGifts());
    }

    /**
     * 新建礼包
     */
    @PostMapping("/gifts")
    public Result<MemberGift> createGift(@Valid @RequestBody MemberGiftRequest request) {
        return Result.success(giftService.createGift(request), "礼包已创建");
    }

    /**
     * 更新礼包
     */
    @PutMapping("/gifts/{giftId}")
    public Result<MemberGift> updateGift(@PathVariable Long giftId,
                                         @Valid @RequestBody MemberGiftRequest request) {
        return Result.success(giftService.updateGift(giftId, request), "礼包已更新");
    }

    /**
     * 下架礼包
     */
    @DeleteMapping("/gifts/{giftId}")
    public Result<Void> disableGift(@PathVariable Long giftId) {
        giftService.disableGift(giftId);
        return Result.success(null, "礼包已下架");
    }

}
