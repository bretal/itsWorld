package com.itsworld.health.controller;

import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.health.dto.CheckupRecordCreateRequest;
import com.itsworld.health.dto.CheckupRecordResponse;
import com.itsworld.health.dto.FeedingRecordCreateRequest;
import com.itsworld.health.dto.FeedingRecordResponse;
import com.itsworld.health.dto.VaccineRecordCreateRequest;
import com.itsworld.health.dto.VaccineRecordResponse;
import com.itsworld.health.dto.WeightRecordCreateRequest;
import com.itsworld.health.dto.WeightRecordResponse;
import com.itsworld.health.service.HealthService;
import com.itsworld.user.entity.User;
import com.itsworld.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 健康管理控制器
 * 提供体重、喂食、疫苗、体检记录的查询与写入接口
 *
 * @author Claude
 * @date 2026-11-05
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;
    private final UserService userService;

    /**
     * 查询体重记录
     * GET /api/health/weight?petId=
     */
    @GetMapping("/weight")
    public Result<List<WeightRecordResponse>> getWeightRecords(@RequestParam Long petId) {
        log.info("查询体重记录: petId={}", petId);
        return Result.success(healthService.getWeightRecords(getCurrentUserId(), petId));
    }

    /**
     * 新增体重记录
     * POST /api/health/weight
     */
    @PostMapping("/weight")
    public Result<Long> addWeightRecord(@Valid @RequestBody WeightRecordCreateRequest request) {
        log.info("新增体重记录: petId={}, weightKg={}", request.getPetId(), request.getWeightKg());
        Long recordId = healthService.addWeightRecord(getCurrentUserId(), request);
        return Result.success(recordId, "体重记录创建成功");
    }

    /**
     * 删除体重记录
     * DELETE /api/health/weight/{recordId}
     */
    @DeleteMapping("/weight/{recordId}")
    public Result<Void> deleteWeightRecord(@PathVariable Long recordId) {
        log.info("删除体重记录: recordId={}", recordId);
        healthService.deleteWeightRecord(getCurrentUserId(), recordId);
        return Result.success(null, "体重记录删除成功");
    }

    /**
     * 查询喂食记录
     * GET /api/health/feeding?petId=&date=yyyy-MM-dd
     */
    @GetMapping("/feeding")
    public Result<List<FeedingRecordResponse>> getFeedingRecords(
            @RequestParam Long petId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("查询喂食记录: petId={}, date={}", petId, date);
        return Result.success(healthService.getFeedingRecords(getCurrentUserId(), petId, date));
    }

    /**
     * 新增喂食记录
     * POST /api/health/feeding
     */
    @PostMapping("/feeding")
    public Result<Long> addFeedingRecord(@Valid @RequestBody FeedingRecordCreateRequest request) {
        log.info("新增喂食记录: petId={}, feedType={}", request.getPetId(), request.getFeedType());
        Long recordId = healthService.addFeedingRecord(getCurrentUserId(), request);
        return Result.success(recordId, "喂食记录创建成功");
    }

    /**
     * 删除喂食记录
     * DELETE /api/health/feeding/{recordId}
     */
    @DeleteMapping("/feeding/{recordId}")
    public Result<Void> deleteFeedingRecord(@PathVariable Long recordId) {
        log.info("删除喂食记录: recordId={}", recordId);
        healthService.deleteFeedingRecord(getCurrentUserId(), recordId);
        return Result.success(null, "喂食记录删除成功");
    }

    /**
     * 查询疫苗记录
     * GET /api/health/vaccine?petId=
     */
    @GetMapping("/vaccine")
    public Result<List<VaccineRecordResponse>> getVaccineRecords(@RequestParam Long petId) {
        log.info("查询疫苗记录: petId={}", petId);
        return Result.success(healthService.getVaccineRecords(getCurrentUserId(), petId));
    }

    /**
     * 新增疫苗记录
     * POST /api/health/vaccine
     */
    @PostMapping("/vaccine")
    public Result<Long> addVaccineRecord(@Valid @RequestBody VaccineRecordCreateRequest request) {
        log.info("新增疫苗记录: petId={}, vaccineName={}", request.getPetId(), request.getVaccineName());
        Long recordId = healthService.addVaccineRecord(getCurrentUserId(), request);
        return Result.success(recordId, "疫苗记录创建成功");
    }

    /**
     * 删除疫苗记录
     * DELETE /api/health/vaccine/{recordId}
     */
    @DeleteMapping("/vaccine/{recordId}")
    public Result<Void> deleteVaccineRecord(@PathVariable Long recordId) {
        log.info("删除疫苗记录: recordId={}", recordId);
        healthService.deleteVaccineRecord(getCurrentUserId(), recordId);
        return Result.success(null, "疫苗记录删除成功");
    }

    /**
     * 查询体检记录
     * GET /api/health/checkup?petId=
     */
    @GetMapping("/checkup")
    public Result<List<CheckupRecordResponse>> getCheckupRecords(@RequestParam Long petId) {
        log.info("查询体检记录: petId={}", petId);
        return Result.success(healthService.getCheckupRecords(getCurrentUserId(), petId));
    }

    /**
     * 新增体检记录
     * POST /api/health/checkup
     */
    @PostMapping("/checkup")
    public Result<Long> addCheckupRecord(@Valid @RequestBody CheckupRecordCreateRequest request) {
        log.info("新增体检记录: petId={}, checkupDate={}", request.getPetId(), request.getCheckupDate());
        Long recordId = healthService.addCheckupRecord(getCurrentUserId(), request);
        return Result.success(recordId, "体检记录创建成功");
    }

    /**
     * 删除体检记录
     * DELETE /api/health/checkup/{recordId}
     */
    @DeleteMapping("/checkup/{recordId}")
    public Result<Void> deleteCheckupRecord(@PathVariable Long recordId) {
        log.info("删除体检记录: recordId={}", recordId);
        healthService.deleteCheckupRecord(getCurrentUserId(), recordId);
        return Result.success(null, "体检记录删除成功");
    }

    /**
     * 分页查询体重记录
     * GET /api/health/weight/page?petId=&page=1&size=20
     */
    @GetMapping("/weight/page")
    public Result<PageResult<WeightRecordResponse>> getWeightRecordPage(
            @RequestParam Long petId,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        log.info("分页查询体重记录: petId={}, page={}, size={}", petId, page, size);
        return Result.success(healthService.getWeightRecordPage(
                getCurrentUserId(), petId,
                PageUtils.normalizePageNum(page),
                PageUtils.normalizePageSize(size)));
    }

    /**
     * 分页查询喂食记录
     * GET /api/health/feeding/page?petId=&date=&page=1&size=20
     */
    @GetMapping("/feeding/page")
    public Result<PageResult<FeedingRecordResponse>> getFeedingRecordPage(
            @RequestParam Long petId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        log.info("分页查询喂食记录: petId={}, date={}, page={}, size={}", petId, date, page, size);
        return Result.success(healthService.getFeedingRecordPage(
                getCurrentUserId(), petId, date,
                PageUtils.normalizePageNum(page),
                PageUtils.normalizePageSize(size)));
    }

    /**
     * 分页查询疫苗记录
     * GET /api/health/vaccine/page?petId=&page=1&size=20
     */
    @GetMapping("/vaccine/page")
    public Result<PageResult<VaccineRecordResponse>> getVaccineRecordPage(
            @RequestParam Long petId,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        log.info("分页查询疫苗记录: petId={}, page={}, size={}", petId, page, size);
        return Result.success(healthService.getVaccineRecordPage(
                getCurrentUserId(), petId,
                PageUtils.normalizePageNum(page),
                PageUtils.normalizePageSize(size)));
    }

    /**
     * 分页查询体检记录
     * GET /api/health/checkup/page?petId=&page=1&size=20
     */
    @GetMapping("/checkup/page")
    public Result<PageResult<CheckupRecordResponse>> getCheckupRecordPage(
            @RequestParam Long petId,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        log.info("分页查询体检记录: petId={}, page={}, size={}", petId, page, size);
        return Result.success(healthService.getCheckupRecordPage(
                getCurrentUserId(), petId,
                PageUtils.normalizePageNum(page),
                PageUtils.normalizePageSize(size)));
    }

    /**
     * 从安全上下文获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        return user.getId();
    }

}
