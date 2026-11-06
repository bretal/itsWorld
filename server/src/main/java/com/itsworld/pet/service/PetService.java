package com.itsworld.pet.service;

import com.itsworld.common.result.PageResult;
import com.itsworld.pet.dto.PetCreateRequest;
import com.itsworld.pet.dto.PetResponse;
import com.itsworld.pet.dto.PetUpdateRequest;
import com.itsworld.pet.entity.Pet;

import java.util.List;

/**
 * 宠物业务接口
 *
 * @author Claude
 * @date 2026-10-30
 */
public interface PetService {

    /**
     * 创建宠物
     *
     * @param userId 用户ID
     * @param createRequest 创建请求
     * @return 宠物ID
     */
    Long createPet(Long userId, PetCreateRequest createRequest);

    /**
     * 获取用户的宠物列表
     *
     * @param userId 用户ID
     * @return 宠物列表
     */
    List<PetResponse> getPetListByUserId(Long userId);

    /**
     * 分页查询宠物列表
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param keyword 名称/品种关键字
     * @return 分页结果
     */
    PageResult<PetResponse> getPetPage(Long userId, long pageNum, long pageSize, String keyword);

    /**
     * 获取单个宠物信息
     *
     * @param userId 用户ID
     * @param petId 宠物ID
     * @return 宠物信息
     */
    PetResponse getPetInfo(Long userId, Long petId);

    /**
     * 更新宠物信息
     *
     * @param userId 用户ID
     * @param petId 宠物ID
     * @param updateRequest 更新请求
     * @return 更新后的宠物信息
     */
    PetResponse updatePet(Long userId, Long petId, PetUpdateRequest updateRequest);

    /**
     * 删除宠物（软删除）
     *
     * @param userId 用户ID
     * @param petId 宠物ID
     */
    void deletePet(Long userId, Long petId);

    /**
     * 根据ID查询宠物（仅内部使用）
     *
     * @param petId 宠物ID
     * @return 宠物对象
     */
    Pet getPetById(Long petId);

}
