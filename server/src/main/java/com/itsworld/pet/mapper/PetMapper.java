package com.itsworld.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.pet.entity.Pet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 宠物数据访问接口
 * 继承MyBatis-Plus的BaseMapper，可直接使用CRUD方法
 *
 * @author Claude
 * @date 2026-10-30
 */
@Mapper
public interface PetMapper extends BaseMapper<Pet> {

    /**
     * 根据用户ID查询宠物列表
     *
     * @param userId 用户ID
     * @return 宠物列表
     */
    @Select("SELECT id, user_id, pet_name, pet_type, breed, birth_date, gender, weight_kg AS weight, avatar_url, bio AS description, is_active AS isActive, care_notes, created_at, updated_at, deleted_at FROM pet_profile WHERE user_id = #{userId} AND deleted_at IS NULL ORDER BY created_at DESC")
    List<Pet> selectByUserId(Long userId);

    /**
     * 根据用户ID和宠物ID查询宠物
     *
     * @param userId 用户ID
     * @param petId 宠物ID
     * @return 宠物对象
     */
    @Select("SELECT id, user_id, pet_name, pet_type, breed, birth_date, gender, weight_kg AS weight, avatar_url, bio AS description, is_active AS isActive, care_notes, created_at, updated_at, deleted_at FROM pet_profile WHERE user_id = #{userId} AND id = #{petId} AND deleted_at IS NULL LIMIT 1")
    Pet selectByUserIdAndPetId(Long userId, Long petId);

    /**
     * 根据用户ID和宠物名称查询宠物
     *
     * @param userId 用户ID
     * @param petName 宠物名称
     * @return 宠物对象
     */
    @Select("SELECT id, user_id, pet_name, pet_type, breed, birth_date, gender, weight_kg AS weight, avatar_url, bio AS description, is_active AS isActive, care_notes, created_at, updated_at, deleted_at FROM pet_profile WHERE user_id = #{userId} AND pet_name = #{petName} AND deleted_at IS NULL LIMIT 1")
    Pet selectByUserIdAndPetName(Long userId, String petName);

}
