package com.itsworld.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.diary.entity.Diary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 日记数据访问接口
 * 继承MyBatis-Plus的BaseMapper，可直接使用CRUD方法
 *
 * @author Claude
 * @date 2026-10-30
 */
@Mapper
public interface DiaryMapper extends BaseMapper<Diary> {

    /**
     * 根据用户ID和宠物ID查询日记列表
     *
     * @param userId 用户ID
     * @param petId 宠物ID
     * @return 日记列表
     */
    @Select("SELECT id, user_id, pet_id, diary_date, morning_mood, question_1 AS question1, question_2 AS question2, question_3 AS question3, question_4 AS question4, question_5 AS question5, question_6 AS question6, content, images_json AS photoUrls, is_published AS isPublic, like_count, comment_count, view_count, created_at, updated_at FROM pet_diary WHERE user_id = #{userId} AND pet_id = #{petId} ORDER BY diary_date DESC")
    List<Diary> selectByUserIdAndPetId(Long userId, Long petId);

    /**
     * 根据用户ID、宠物ID和日期查询日记
     *
     * @param userId 用户ID
     * @param petId 宠物ID
     * @param diaryDate 日期
     * @return 日记对象
     */
    @Select("SELECT id, user_id, pet_id, diary_date, morning_mood, question_1 AS question1, question_2 AS question2, question_3 AS question3, question_4 AS question4, question_5 AS question5, question_6 AS question6, content, images_json AS photoUrls, is_published AS isPublic, like_count, comment_count, view_count, created_at, updated_at FROM pet_diary WHERE user_id = #{userId} AND pet_id = #{petId} AND diary_date = #{diaryDate} LIMIT 1")
    Diary selectByUserIdPetIdAndDate(Long userId, Long petId, String diaryDate);

    /**
     * 根据用户ID查询所有日记（分页）
     *
     * @param userId 用户ID
     * @return 日记列表
     */
    @Select("SELECT id, user_id, pet_id, diary_date, morning_mood, question_1 AS question1, question_2 AS question2, question_3 AS question3, question_4 AS question4, question_5 AS question5, question_6 AS question6, content, images_json AS photoUrls, is_published AS isPublic, like_count, comment_count, view_count, created_at, updated_at FROM pet_diary WHERE user_id = #{userId} ORDER BY diary_date DESC")
    List<Diary> selectByUserId(Long userId);

}
