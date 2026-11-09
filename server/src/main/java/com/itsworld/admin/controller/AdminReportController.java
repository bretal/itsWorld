package com.itsworld.admin.controller;

import com.itsworld.admin.service.AdminDataService;
import com.itsworld.common.exception.BusinessException;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 管理员登记外部反馈、处理举报并查看操作记录。 */
@RestController
@RequestMapping("/api/admin/moderation")
@RequiredArgsConstructor
public class AdminReportController {
    private final JdbcTemplate jdbc;
    private final AdminDataService data;
    private final CurrentUserService currentUser;
    public record Report(@Pattern(regexp="posts|comments|diaries") @NotNull String targetType,
                         @NotNull @Positive Long targetId, Long reporterId,
                         @NotBlank @Size(max=500) String reason) { }
    public record Resolution(@Pattern(regexp="RESOLVED|DISMISSED") @NotNull String status,
                             @NotBlank @Size(max=500) String resolution) { }
    @GetMapping("/reports")
    public Result<PageResult<Map<String,Object>>> reports(@RequestParam(required=false) String status,
             @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        int current=Math.max(1,page), limit=Math.min(100,Math.max(1,size));
        String where=status == null || status.isBlank() ? "" : " WHERE status=?";
        List<Object> args=new ArrayList<>(); if(!where.isEmpty()) { args.add(status); }
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM content_report"+where,Long.class,args.toArray());
        args.add(limit); args.add((long)(current-1)*limit);
        return Result.success(PageResult.of(jdbc.queryForList("SELECT * FROM content_report"+where+" ORDER BY id DESC LIMIT ? OFFSET ?",args.toArray()),total == null ? 0 : total,current,limit));
    }
    @PostMapping("/reports")
    public Result<Void> create(@Valid @RequestBody Report request) {
        data.detail(request.targetType(),request.targetId());
        jdbc.update("INSERT INTO content_report(target_type,target_id,reporter_id,reason) VALUES(?,?,?,?)",request.targetType(),request.targetId(),request.reporterId(),request.reason());
        return Result.success(null);
    }
    @PostMapping("/reports/{id}/resolve")
    @Transactional
    public Result<Void> resolve(@PathVariable long id,@Valid @RequestBody Resolution request) {
        int changed=jdbc.update("UPDATE content_report SET status=?,resolution=?,handled_by=?,handled_at=NOW() WHERE id=? AND status='PENDING'",request.status(),request.resolution(),currentUser.getUserId(),id);
        if(changed!=1) { throw new BusinessException("举报不存在或已经处理"); }
        return Result.success(null);
    }
    @GetMapping("/logs")
    public Result<List<Map<String,Object>>> logs(@RequestParam String targetType,@RequestParam long targetId) {
        return Result.success(jdbc.queryForList("SELECT * FROM admin_moderation_log WHERE target_type=? AND target_id=? ORDER BY id DESC LIMIT 100",targetType,targetId));
    }
}
