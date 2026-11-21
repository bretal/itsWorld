package com.itsworld.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.diary.dto.DiaryCreateRequest;
import com.itsworld.diary.dto.DiaryResponse;
import com.itsworld.diary.entity.Diary;
import com.itsworld.diary.mapper.DiaryMapper;
import com.itsworld.member.service.MemberTaskService;
import com.itsworld.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 日记业务实现
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryMapper diaryMapper;
    private final PetService petService;
    private final MemberTaskService memberTaskService;
    private final ObjectMapper objectMapper;

    /**
     * 创建日记
     */
    @Transactional
    public Long createDiary(Long userId, DiaryCreateRequest createRequest) {
        // 1. 验证宠物是否存在
        petService.getPetInfo(userId, createRequest.getPetId());

        // 同一宠物同一天可创建多条独立记录。
        // 3. 创建日记
        Diary diary = Diary.builder()
                .userId(userId)
                .petId(createRequest.getPetId())
                .diaryDate(createRequest.getDiaryDate())
                .occurredAt(createRequest.getOccurredAt())
                .category(createRequest.getCategory())
                .morningMood(createRequest.getMood())
                .question1(createRequest.getLocation())
                .question2(joinActivities(createRequest.getActivities()))
                .question3(toHealthScoreText(createRequest.getHealthScore()))
                .content(createRequest.getContent())
                .photoUrls(serializePhotoUrls(createRequest.getPhotoUrls()))
                .isPublic(createRequest.getIsPublic() == null ? 1 : createRequest.getIsPublic())
                .likeCount(0)
                .commentCount(0)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 4. 保存到数据库
        int result = diaryMapper.insert(diary);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        // 会员任务进度：记录一篇日记
        memberTaskService.recordProgress(userId, "DIARY_CREATE");

        log.info("日记创建成功: userId={}, petId={}, diaryDate={}", userId, createRequest.getPetId(), createRequest.getDiaryDate());
        return diary.getId();
    }

    /**
     * 获取宠物的日记列表
     */
    public List<DiaryResponse> getDiaryList(Long userId, Long petId) {
        // 验证宠物是否存在
        petService.getPetInfo(userId, petId);

        List<Diary> diaries = diaryMapper.selectByUserIdAndPetId(userId, petId);
        return convertWithLikes(userId, diaries);
    }

    public PageResult<DiaryResponse> getDiaryPage(Long userId, Long petId, long pageNum,
                                                  long pageSize, String category) {
        petService.getPetInfo(userId, petId);

        Page<Diary> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<Diary>()
                .eq(Diary::getUserId, userId)
                .eq(Diary::getPetId, petId)
                .orderByDesc(Diary::getDiaryDate)
                .orderByDesc(Diary::getId);
        if (category != null && !category.isBlank()
                && !"undefined".equalsIgnoreCase(category)
                && !"null".equalsIgnoreCase(category)) {
            wrapper.eq(Diary::getCategory, category);
        }

        Page<Diary> resultPage = diaryMapper.selectPage(page, wrapper);
        List<DiaryResponse> records = convertWithLikes(userId, resultPage.getRecords());
        return PageResult.of(records, resultPage.getTotal(),
                resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 获取单条日记
     */
    public DiaryResponse getDiaryInfo(Long userId, Long diaryId) {
        Diary diary = diaryMapper.selectById(diaryId);
        if (diary == null || !diary.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }
        var response = convertToDiaryResponse(diary);
        response.setLiked(diaryMapper.liked(diaryId, userId) > 0);
        return response;
    }

    /**
     * 获取某日期的日记
     */
    public DiaryResponse getDiaryByDate(Long userId, Long petId, String diaryDate) {
        // 验证宠物是否存在
        petService.getPetInfo(userId, petId);

        Diary diary = diaryMapper.selectByUserIdPetIdAndDate(userId, petId, diaryDate);
        if (diary == null) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }
        var response = convertToDiaryResponse(diary);
        response.setLiked(diaryMapper.liked(diary.getId(), userId) > 0);
        return response;
    }

    /**
     * 更新日记
     */
    @Transactional
    public DiaryResponse updateDiary(Long userId, Long diaryId, DiaryCreateRequest createRequest) {
        Diary diary = diaryMapper.selectById(diaryId);
        if (diary == null || !diary.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        if (createRequest.getContent() != null) {
            diary.setContent(createRequest.getContent());
        }
        if (createRequest.getOccurredAt() != null) {
            diary.setOccurredAt(createRequest.getOccurredAt());
            diary.setDiaryDate(createRequest.getOccurredAt().toLocalDate().toString());
        }
        if (createRequest.getCategory() != null) {
            diary.setCategory(createRequest.getCategory());
        }
        if (createRequest.getMood() != null) {
            diary.setMorningMood(createRequest.getMood());
        }
        if (createRequest.getLocation() != null) {
            diary.setQuestion1(createRequest.getLocation());
        }
        if (createRequest.getActivities() != null) {
            diary.setQuestion2(joinActivities(createRequest.getActivities()));
        }
        if (createRequest.getHealthScore() != null) {
            diary.setQuestion3(toHealthScoreText(createRequest.getHealthScore()));
        }
        if (createRequest.getPhotoUrls() != null) {
            diary.setPhotoUrls(serializePhotoUrls(createRequest.getPhotoUrls()));
        }
        if (createRequest.getIsPublic() != null) {
            diary.setIsPublic(createRequest.getIsPublic());
        }

        diary.setUpdatedAt(LocalDateTime.now());
        diaryMapper.updateById(diary);

        log.info("日记更新成功: userId={}, diaryId={}", userId, diaryId);
        return convertToDiaryResponse(diary);
    }

    /**
     * 删除日记
     */
    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        Diary diary = diaryMapper.selectById(diaryId);
        if (diary == null || !diary.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        // pet_diary 没有 deleted_at 列，采用硬删除；同时避免与 uk_pet_date 唯一键冲突
        diaryMapper.deleteById(diaryId);

        log.info("日记删除成功: userId={}, diaryId={}", userId, diaryId);
    }

    /**
     * 点赞日记
     */
    @Transactional
    public void likeDiary(Long userId, Long diaryId) {
        Diary diary = diaryMapper.lockDiary(diaryId);
        if (diary == null) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        if (diaryMapper.liked(diaryId, userId) == 0) {
            diaryMapper.addLike(diaryId, userId);
            diaryMapper.changeLikeCount(diaryId, 1);
        }

        log.info("日记点赞成功: diaryId={}", diaryId);
    }

    @Transactional
    public void unlikeDiary(Long userId, Long diaryId) {
        if (diaryMapper.lockDiary(diaryId) == null) throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        if (diaryMapper.removeLike(diaryId, userId) > 0) diaryMapper.changeLikeCount(diaryId, -1);
    }

    /**
     * 获取用户所有日记
     */
    public List<DiaryResponse> getUserDiaries(Long userId) {
        List<Diary> diaries = diaryMapper.selectByUserId(userId);
        return convertWithLikes(userId, diaries);
    }

    private List<DiaryResponse> convertWithLikes(Long userId, List<Diary> diaries) {
        if (diaries.isEmpty()) return List.of();
        var ids = diaries.stream().map(Diary::getId).collect(Collectors.toList());
        var likedIds = new HashSet<>(diaryMapper.selectLikedDiaryIds(userId, ids));
        return diaries.stream().map(diary -> {
            var response = convertToDiaryResponse(diary);
            response.setLiked(likedIds.contains(diary.getId()));
            return response;
        }).collect(Collectors.toList());
    }

    private String joinActivities(List<String> activities) {
        if (activities == null || activities.isEmpty()) {
            return null;
        }
        return String.join(",", activities);
    }

    private String toHealthScoreText(Integer healthScore) {
        return healthScore == null ? null : String.valueOf(healthScore);
    }

    private String serializePhotoUrls(List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(photoUrls);
        } catch (Exception e) {
            log.warn("序列化照片列表失败", e);
            return "[]";
        }
    }

    private Integer parseHealthScore(String healthScoreText) {
        if (healthScoreText == null || healthScoreText.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(healthScoreText);
        } catch (NumberFormatException e) {
            log.warn("解析健康评分失败: {}", healthScoreText);
            return 0;
        }
    }

    /**
     * 将Diary实体转换为DiaryResponse DTO
     */
    private DiaryResponse convertToDiaryResponse(Diary diary) {
        List<String> photoUrls = null;
        if (diary.getPhotoUrls() != null && !diary.getPhotoUrls().isEmpty() && !diary.getPhotoUrls().equals("[]")) {
            try {
                photoUrls = objectMapper.readValue(diary.getPhotoUrls(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("反序列化照片列表失败", e);
                photoUrls = List.of();
            }
        }

        return DiaryResponse.builder()
                .id(diary.getId())
                .userId(diary.getUserId())
                .petId(diary.getPetId())
                .diaryDate(diary.getDiaryDate())
                .occurredAt(diary.getOccurredAt())
                .location(diary.getQuestion1())
                .category(diary.getCategory())
                .greeting(buildGreeting(diary))
                .healthScore(parseHealthScore(diary.getQuestion3()))
                .content(diary.getContent())
                .photoUrls(photoUrls)
                .likeCount(diary.getLikeCount())
                .commentCount(diary.getCommentCount())
                .isPublic(diary.getIsPublic())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }

    private String buildGreeting(Diary diary) {
        String location = diary.getQuestion1();
        String mood = diary.getMorningMood();
        if (location != null && mood != null) {
            return location + " · " + mood;
        }
        if (location != null) {
            return location;
        }
        if (mood != null) {
            return mood;
        }
        return "今日问候";
    }

}
