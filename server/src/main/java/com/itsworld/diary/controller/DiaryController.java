package com.itsworld.diary.controller;

import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.diary.dto.DiaryCreateRequest;
import com.itsworld.diary.dto.DiaryResponse;
import com.itsworld.diary.service.DiaryService;
import com.itsworld.user.entity.User;
import com.itsworld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 日记控制器
 * 处理日记的CRUD操作
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final UserService userService;

    /**
     * 创建日记
     * POST /api/diary
     *
     * @param createRequest 创建请求
     * @return 日记ID
     */
    @PostMapping
    public Result<Long> createDiary(@Valid @RequestBody DiaryCreateRequest createRequest) {
        log.info("创建日记: petId={}, diaryDate={}", createRequest.getPetId(), createRequest.getDiaryDate());

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        Long diaryId = diaryService.createDiary(user.getId(), createRequest);
        return Result.success(diaryId, "日记创建成功");
    }

    /**
     * 获取宠物的日记列表
     * GET /api/diary?petId={petId}
     *
     * @param petId 宠物ID
     * @return 日记列表
     */
    @GetMapping
    public Result<List<DiaryResponse>> getDiaryList(@RequestParam Long petId) {
        log.info("获取宠物日记列表: petId={}", petId);

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        List<DiaryResponse> diaries = diaryService.getDiaryList(user.getId(), petId);
        return Result.success(diaries);
    }

    /**
     * 分页查询宠物日记
     * GET /api/diary/page?petId=&page=1&size=20&category=
     *
     * @param petId 宠物ID
     * @param page 页码
     * @param size 每页条数
     * @param category 分类
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<PageResult<DiaryResponse>> getDiaryPage(
            @RequestParam Long petId,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String category) {
        log.info("分页查询宠物日记: petId={}, page={}, size={}, category={}",
                petId, page, size, category);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        PageResult<DiaryResponse> result = diaryService.getDiaryPage(
                user.getId(), petId,
                PageUtils.normalizePageNum(page),
                PageUtils.normalizePageSize(size),
                category);
        return Result.success(result);
    }

    /**
     * 获取单条日记
     * GET /api/diary/{diaryId}
     *
     * @param diaryId 日记ID
     * @return 日记信息
     */
    @GetMapping("/{diaryId}")
    public Result<DiaryResponse> getDiaryInfo(@PathVariable Long diaryId) {
        log.info("获取日记信息: diaryId={}", diaryId);

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        DiaryResponse diaryInfo = diaryService.getDiaryInfo(user.getId(), diaryId);
        return Result.success(diaryInfo);
    }

    /**
     * 获取某日期的日记
     * GET /api/diary/date/{petId}/{diaryDate}
     *
     * @param petId 宠物ID
     * @param diaryDate 日期
     * @return 日记信息
     */
    @GetMapping("/date/{petId}/{diaryDate}")
    public Result<DiaryResponse> getDiaryByDate(
            @PathVariable Long petId,
            @PathVariable String diaryDate) {
        log.info("获取指定日期日记: petId={}, diaryDate={}", petId, diaryDate);

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        DiaryResponse diaryInfo = diaryService.getDiaryByDate(user.getId(), petId, diaryDate);
        return Result.success(diaryInfo);
    }

    /**
     * 更新日记
     * PUT /api/diary/{diaryId}
     *
     * @param diaryId 日记ID
     * @param updateRequest 更新请求
     * @return 更新后的日记
     */
    @PutMapping("/{diaryId}")
    public Result<DiaryResponse> updateDiary(
            @PathVariable Long diaryId,
            @RequestBody DiaryCreateRequest updateRequest) {
        log.info("更新日记: diaryId={}", diaryId);

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        DiaryResponse updatedDiary = diaryService.updateDiary(user.getId(), diaryId, updateRequest);
        return Result.success(updatedDiary, "日记更新成功");
    }

    /**
     * 删除日记
     * DELETE /api/diary/{diaryId}
     *
     * @param diaryId 日记ID
     * @return 删除结果
     */
    @DeleteMapping("/{diaryId}")
    public Result<Void> deleteDiary(@PathVariable Long diaryId) {
        log.info("删除日记: diaryId={}", diaryId);

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        diaryService.deleteDiary(user.getId(), diaryId);
        return Result.success(null, "日记删除成功");
    }

    /**
     * 点赞日记
     * POST /api/diary/{diaryId}/like
     *
     * @param diaryId 日记ID
     * @return 点赞结果
     */
    @PostMapping("/{diaryId}/like")
    public Result<Void> likeDiary(@PathVariable Long diaryId) {
        log.info("点赞日记: diaryId={}", diaryId);

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        diaryService.likeDiary(user.getId(), diaryId);
        return Result.success(null, "点赞成功");
    }

    /**
     * 获取用户所有日记
     * GET /api/diary/all
     *
     * @return 日记列表
     */
    @GetMapping("/all")
    public Result<List<DiaryResponse>> getUserDiaries() {
        log.info("获取用户所有日记");

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        List<DiaryResponse> diaries = diaryService.getUserDiaries(user.getId());
        return Result.success(diaries);
    }

}
