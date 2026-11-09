package com.itsworld.admin.controller;

import com.itsworld.admin.service.AdminDataService;
import com.itsworld.common.result.Result;
import com.itsworld.common.result.PageResult;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/** 平台级业务记录管理，受 /api/admin/** 的 ADMIN 角色规则保护。 */
@RestController
@RequestMapping("/api/admin/data")
@RequiredArgsConstructor
public class AdminDataController {
    private final AdminDataService service;
    private final CurrentUserService currentUser;
    public record Moderation(@NotBlank String action, @NotBlank @Size(max=500) String reason) { }
    @GetMapping("/{kind}")
    public Result<PageResult<Map<String,Object>>> list(@PathVariable String kind,
            @RequestParam(required=false) Long userId, @RequestParam(required=false) Long petId,
            @RequestParam(required=false) Long postId, @RequestParam(required=false) String keyword,
            @RequestParam(required=false) Long recordId,
            @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        return Result.success(service.list(kind,userId,petId,postId,keyword,recordId,page,size));
    }
    @GetMapping("/{kind}/{id}")
    public Result<Map<String,Object>> detail(@PathVariable String kind,@PathVariable long id) {
        return Result.success(service.detail(kind,id));
    }
    @PostMapping("/{kind}/{id}/moderate")
    public Result<Void> moderate(@PathVariable String kind,@PathVariable long id,@Valid @RequestBody Moderation request) {
        service.moderate(kind,id,request.action(),request.reason(),currentUser.getUserId());
        return Result.success(null);
    }
}
