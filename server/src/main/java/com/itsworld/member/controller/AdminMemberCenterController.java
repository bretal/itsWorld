package com.itsworld.member.controller;

import com.itsworld.member.service.MemberCenterService;
import com.itsworld.member.dto.MemberCenterRequests.*;
import com.itsworld.security.service.CurrentUserService;
import com.itsworld.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 管理端会员方案、礼品、兑换履约接口。由 /api/admin/** 角色规则保护。 */
@RestController
@RequestMapping("/api/admin/member")
@RequiredArgsConstructor
public class AdminMemberCenterController {
    private final MemberCenterService service;
    private final CurrentUserService currentUser;
    @GetMapping("/subscription-plans")
    public Result<List<Map<String,Object>>> plans() { return Result.success(service.plans(true)); }
    @PostMapping("/subscription-plans")
    public Result<Void> createPlan(@Valid @RequestBody Plan p) { service.savePlan(null,p); return Result.success(null); }
    @PutMapping("/subscription-plans/{id}")
    public Result<Void> updatePlan(@PathVariable Long id,@Valid @RequestBody Plan p) { service.savePlan(id,p); return Result.success(null); }
    @GetMapping("/reward-products")
    public Result<List<Map<String,Object>>> rewards() { return Result.success(service.rewards(true)); }
    @PostMapping("/reward-products")
    public Result<Void> createReward(@Valid @RequestBody Reward p) { service.saveReward(null,p); return Result.success(null); }
    @PutMapping("/reward-products/{id}")
    public Result<Void> updateReward(@PathVariable Long id,@Valid @RequestBody Reward p) { service.saveReward(id,p); return Result.success(null); }
    @GetMapping("/redemptions")
    public Result<List<Map<String,Object>>> redemptions(@RequestParam(required=false) Long userId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return Result.success(service.redemptions(userId,page,size));
    }
    @PostMapping("/redemptions/{no}/fulfill")
    public Result<Void> fulfill(@PathVariable String no) { service.fulfill(no,currentUser.getUserId()); return Result.success(null); }
}

