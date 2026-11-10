package com.itsworld.admin.controller;

import com.itsworld.admin.service.AdminReportService;
import com.itsworld.common.result.Result;
import com.itsworld.common.result.PageResult;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
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

/** 管理员登记外部反馈、处理举报并查看操作记录。 */
@RestController
@RequestMapping("/api/admin/moderation")
@RequiredArgsConstructor
public class AdminReportController {
    private final AdminReportService service;
    private final CurrentUserService currentUser;
    public record Report(@Pattern(regexp="posts|comments|diaries") @NotNull String targetType,
                         @NotNull @Positive Long targetId, Long reporterId,
                         @NotBlank @Size(max=500) String reason) { }
    public record Resolution(@Pattern(regexp="RESOLVED|DISMISSED") @NotNull String status,
                             @NotBlank @Size(max=500) String resolution) { }
    @GetMapping("/reports")
    public Result<PageResult<Map<String,Object>>> reports(@RequestParam(required=false) String status,
             @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return Result.success(service.reports(status, page, size));
    }
    @PostMapping("/reports")
    public Result<Void> create(@Valid @RequestBody Report request) {
        service.create(request.targetType(), request.targetId(), request.reporterId(), request.reason());
        return Result.success(null);
    }
    @PostMapping("/reports/{id}/resolve")
    public Result<Void> resolve(@PathVariable long id,@Valid @RequestBody Resolution request) {
        service.resolve(id, request.status(), request.resolution(), currentUser.getUserId());
        return Result.success(null);
    }
    @GetMapping("/logs")
    public Result<List<Map<String,Object>>> logs(@RequestParam String targetType,@RequestParam long targetId) {
        return Result.success(service.logs(targetType, targetId));
    }
}
