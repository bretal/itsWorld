package com.itsworld.member.controller;

import com.itsworld.member.service.MemberCenterService;
import com.itsworld.member.dto.MemberCenterRequests.*;
import com.itsworld.security.service.CurrentUserService;
import com.itsworld.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 会员购买续费、成长值兑换用户接口。 */
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberCenterController {
    private final MemberCenterService service;
    private final CurrentUserService currentUser;
    @GetMapping("/subscription-plans")
    public Result<List<Map<String,Object>>> plans() { return Result.success(service.plans(false)); }
    @PostMapping("/subscription-orders")
    public Result<String> purchase(@Valid @RequestBody Purchase p) { return Result.success(service.purchase(currentUser.getUserId(),p)); }
    @GetMapping("/reward-products")
    public Result<List<Map<String,Object>>> rewards() { return Result.success(service.rewards(false)); }
    @PostMapping("/redemptions")
    public Result<String> redeem(@Valid @RequestBody Redeem p) { return Result.success(service.redeem(currentUser.getUserId(),p)); }
    @GetMapping("/redemptions")
    public Result<List<Map<String,Object>>> redemptions(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return Result.success(service.redemptions(currentUser.getUserId(),page,size));
    }
}
