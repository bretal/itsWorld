package com.itsworld.pet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.member.service.MemberTaskService;
import com.itsworld.pet.dto.PetCreateRequest;
import com.itsworld.pet.dto.PetResponse;
import com.itsworld.pet.dto.PetUpdateRequest;
import com.itsworld.pet.entity.Pet;
import com.itsworld.pet.mapper.PetMapper;
import com.itsworld.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 宠物业务实现
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetMapper petMapper;
    private final MemberTaskService memberTaskService;

    /**
     * 创建宠物
     */
    @Override
    @Transactional
    public Long createPet(Long userId, PetCreateRequest createRequest) {
        // 1. 检查宠物名称是否已存在
        Pet existingPet = petMapper.selectByUserIdAndPetName(userId, createRequest.getPetName());
        if (existingPet != null) {
            throw new BusinessException(ErrorCode.PET_ALREADY_EXISTS);
        }

        // 2. 创建新宠物
        Pet pet = Pet.builder()
                .userId(userId)
                .petName(createRequest.getPetName())
                .petType(createRequest.getPetType())
                .avatarUrl(createRequest.getAvatarUrl())
                .breed(createRequest.getBreed())
                .gender(createRequest.getGender())
                .birthDate(createRequest.getBirthDate())
                .weight(createRequest.getWeight())
                .height(createRequest.getHeight())
                .description(createRequest.getDescription())
                .isActive(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 3. 保存到数据库
        int result = petMapper.insert(pet);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        // 会员任务进度：完善宠物档案
        memberTaskService.recordProgress(userId, "PET_CREATE");

        log.info("宠物创建成功: userId={}, petName={}", userId, createRequest.getPetName());
        return pet.getId();
    }

    /**
     * 获取用户的宠物列表
     */
    @Override
    public List<PetResponse> getPetListByUserId(Long userId) {
        List<Pet> pets = petMapper.selectByUserId(userId);
        return pets.stream()
                .map(this::convertToPetResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<PetResponse> getPetPage(Long userId, long pageNum, long pageSize, String keyword) {
        Page<Pet> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId)
                .isNull(Pet::getDeletedAt)
                .orderByDesc(Pet::getCreatedAt)
                .orderByDesc(Pet::getId);
        if (keyword != null && !keyword.isBlank()
                && !"undefined".equalsIgnoreCase(keyword)
                && !"null".equalsIgnoreCase(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(condition -> condition
                    .like(Pet::getPetName, trimmedKeyword)
                    .or()
                    .like(Pet::getBreed, trimmedKeyword));
        }

        Page<Pet> resultPage = petMapper.selectPage(page, wrapper);
        List<PetResponse> records = resultPage.getRecords().stream()
                .map(this::convertToPetResponse)
                .collect(Collectors.toList());
        return PageResult.of(records, resultPage.getTotal(),
                resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 获取单个宠物信息
     */
    @Override
    public PetResponse getPetInfo(Long userId, Long petId) {
        Pet pet = petMapper.selectByUserIdAndPetId(userId, petId);
        if (pet == null) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }
        return convertToPetResponse(pet);
    }

    /**
     * 更新宠物信息
     */
    @Override
    @Transactional
    public PetResponse updatePet(Long userId, Long petId, PetUpdateRequest updateRequest) {
        Pet pet = petMapper.selectByUserIdAndPetId(userId, petId);
        if (pet == null) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }

        // 更新允许修改的字段
        if (updateRequest.getPetName() != null) {
            pet.setPetName(updateRequest.getPetName());
        }
        if (updateRequest.getAvatarUrl() != null) {
            pet.setAvatarUrl(updateRequest.getAvatarUrl());
        }
        if (updateRequest.getBreed() != null) {
            pet.setBreed(updateRequest.getBreed());
        }
        if (updateRequest.getGender() != null) {
            pet.setGender(updateRequest.getGender());
        }
        if (updateRequest.getBirthDate() != null) {
            pet.setBirthDate(updateRequest.getBirthDate());
        }
        if (updateRequest.getWeight() != null) {
            pet.setWeight(updateRequest.getWeight());
        }
        if (updateRequest.getHeight() != null) {
            pet.setHeight(updateRequest.getHeight());
        }
        if (updateRequest.getDescription() != null) {
            pet.setDescription(updateRequest.getDescription());
        }

        pet.setUpdatedAt(LocalDateTime.now());
        petMapper.updateById(pet);

        log.info("宠物信息更新成功: userId={}, petId={}", userId, petId);
        return convertToPetResponse(pet);
    }

    /**
     * 删除宠物（软删除）
     */
    @Override
    @Transactional
    public void deletePet(Long userId, Long petId) {
        Pet pet = petMapper.selectByUserIdAndPetId(userId, petId);
        if (pet == null) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }

        // 逻辑删除（deleted_at）
        petMapper.deleteById(petId);

        log.info("宠物删除成功: userId={}, petId={}", userId, petId);
    }

    /**
     * 根据ID查询宠物
     */
    @Override
    public Pet getPetById(Long petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }
        return pet;
    }

    /**
     * 将Pet实体转换为PetResponse DTO
     */
    private PetResponse convertToPetResponse(Pet pet) {
        return PetResponse.builder()
                .id(pet.getId())
                .userId(pet.getUserId())
                .petName(pet.getPetName())
                .avatarUrl(pet.getAvatarUrl())
                .petType(pet.getPetType())
                .breed(pet.getBreed())
                .gender(pet.getGender())
                .birthDate(pet.getBirthDate())
                .weight(pet.getWeight())
                .height(pet.getHeight())
                .description(pet.getDescription())
                .build();
    }

}
