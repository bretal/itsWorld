package com.itsworld.pet.controller;

import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.pet.dto.PetCreateRequest;
import com.itsworld.pet.dto.PetResponse;
import com.itsworld.pet.dto.PetUpdateRequest;
import com.itsworld.pet.service.PetService;
import com.itsworld.user.entity.User;
import com.itsworld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 宠物控制器
 * 处理宠物档案的CRUD操作
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final UserService userService;

    /**
     * 创建宠物档案
     * POST /api/pet
     *
     * @param createRequest 创建请求
     * @return 宠物ID
     */
    @PostMapping
    public Result<Long> createPet(@Valid @RequestBody PetCreateRequest createRequest) {
        log.info("创建宠物档案: petName={}", createRequest.getPetName());

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        Long petId = petService.createPet(user.getId(), createRequest);
        return Result.success(petId, "宠物创建成功");
    }

    /**
     * 获取宠物列表
     * GET /api/pet
     *
     * @return 宠物列表
     */
    @GetMapping
    public Result<List<PetResponse>> getPetList() {
        log.info("获取宠物列表");

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        List<PetResponse> pets = petService.getPetListByUserId(user.getId());
        return Result.success(pets);
    }

    /**
     * 分页查询宠物列表
     * GET /api/pet/page?page=1&size=20&keyword=
     *
     * @param page 页码
     * @param size 每页条数
     * @param keyword 名称/品种关键字
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<PageResult<PetResponse>> getPetPage(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String keyword) {
        log.info("分页查询宠物列表: page={}, size={}, keyword={}", page, size, keyword);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        PageResult<PetResponse> result = petService.getPetPage(
                user.getId(),
                PageUtils.normalizePageNum(page),
                PageUtils.normalizePageSize(size),
                keyword);
        return Result.success(result);
    }

    /**
     * 获取单个宠物信息
     * GET /api/pet/{petId}
     *
     * @param petId 宠物ID
     * @return 宠物信息
     */
    @GetMapping("/{petId}")
    public Result<PetResponse> getPetInfo(@PathVariable Long petId) {
        log.info("获取宠物信息: petId={}", petId);

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        PetResponse petInfo = petService.getPetInfo(user.getId(), petId);
        return Result.success(petInfo);
    }

    /**
     * 更新宠物信息
     * PUT /api/pet/{petId}
     *
     * @param petId 宠物ID
     * @param updateRequest 更新请求
     * @return 更新后的宠物信息
     */
    @PutMapping("/{petId}")
    public Result<PetResponse> updatePet(
            @PathVariable Long petId,
            @RequestBody PetUpdateRequest updateRequest) {
        log.info("更新宠物信息: petId={}", petId);

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        PetResponse updatedPet = petService.updatePet(user.getId(), petId, updateRequest);
        return Result.success(updatedPet, "宠物信息更新成功");
    }

    /**
     * 删除宠物
     * DELETE /api/pet/{petId}
     *
     * @param petId 宠物ID
     * @return 删除结果
     */
    @DeleteMapping("/{petId}")
    public Result<Void> deletePet(@PathVariable Long petId) {
        log.info("删除宠物: petId={}", petId);

        // 获取当前登录用户ID
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);

        petService.deletePet(user.getId(), petId);
        return Result.success(null, "宠物删除成功");
    }

}
