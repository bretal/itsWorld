package com.itsworld.diary.service;

import com.itsworld.diary.dto.DiaryCreateRequest;
import com.itsworld.diary.dto.DiaryResponse;
import com.itsworld.diary.entity.Diary;

import java.util.List;

/**
 * 日记业务接口
 *
 * @author Claude
 * @date 2026-10-30
 */
public interface DiaryService {

    /**
     * 创建日记
     *
     * @param userId 用户ID
     * @param createRequest 创建请求
     * @return 日记ID
     */
    Long createDiary(Long userId, DiaryCreateRequest createRequest);

    /**
     * 获取宠物的日记列表
     *
     * @param userId 用户ID
     * @param petId 宠物ID
     * @return 日记列表
     */
    List<DiaryResponse> getDiaryList(Long userId, Long petId);

    /**
     * 获取单条日记
     *
     * @param userId 用户ID
     * @param diaryId 日记ID
     * @return 日记信息
     */
    DiaryResponse getDiaryInfo(Long userId, Long diaryId);

    /**
     * 获取某日期的日记
     *
     * @param userId 用户ID
     * @param petId 宠物ID
     * @param diaryDate 日期
     * @return 日记信息
     */
    DiaryResponse getDiaryByDate(Long userId, Long petId, String diaryDate);

    /**
     * 更新日记
     *
     * @param userId 用户ID
     * @param diaryId 日记ID
     * @param createRequest 更新请求
     * @return 更新后的日记
     */
    DiaryResponse updateDiary(Long userId, Long diaryId, DiaryCreateRequest createRequest);

    /**
     * 删除日记
     *
     * @param userId 用户ID
     * @param diaryId 日记ID
     */
    void deleteDiary(Long userId, Long diaryId);

    /**
     * 点赞日记
     *
     * @param userId 用户ID
     * @param diaryId 日记ID
     */
    void likeDiary(Long userId, Long diaryId);

    /**
     * 获取用户所有日记
     *
     * @param userId 用户ID
     * @return 日记列表
     */
    List<DiaryResponse> getUserDiaries(Long userId);

}
