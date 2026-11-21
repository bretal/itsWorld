package com.itsworld.diary.service;

import com.itsworld.diary.entity.Diary;
import com.itsworld.diary.mapper.DiaryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DiaryLikeTest {
    @Test
    void listsReadLikesForCurrentUserInOneBatchAndKeepCounts() {
        var mapper = mock(DiaryMapper.class);
        var pets = mock(com.itsworld.pet.service.PetService.class);
        var service = new DiaryService(mapper, pets, null, new ObjectMapper());
        var first = Diary.builder().id(10L).userId(1L).likeCount(3).build();
        var second = Diary.builder().id(11L).userId(1L).likeCount(2).build();
        var diaries = java.util.List.of(first, second);
        when(mapper.selectByUserIdAndPetId(1L, 20L)).thenReturn(diaries);
        when(mapper.selectLikedDiaryIds(1L, java.util.List.of(10L, 11L)))
                .thenReturn(java.util.List.of(10L));
        var responses = service.getDiaryList(1L, 20L);
        assertTrue(responses.get(0).isLiked());
        assertFalse(responses.get(1).isLiked());
        assertEquals(3, responses.get(0).getLikeCount());
        verify(mapper, times(1)).selectLikedDiaryIds(1L, java.util.List.of(10L, 11L));
        verify(mapper, never()).liked(anyLong(), anyLong());

        when(mapper.selectPage(any(), any())).thenAnswer(invocation -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Diary> page = invocation.getArgument(0);
            page.setRecords(diaries);
            page.setTotal(2);
            return page;
        });
        var page = service.getDiaryPage(1L, 20L, 1, 10, "daily");
        assertTrue(page.getRecords().get(0).isLiked());
        assertFalse(page.getRecords().get(1).isLiked());
        assertEquals(3, page.getRecords().get(0).getLikeCount());

        when(mapper.selectByUserIdAndPetId(1L, 20L)).thenReturn(java.util.List.of());
        assertTrue(service.getDiaryList(1L, 20L).isEmpty());
        verify(mapper, times(2)).selectLikedDiaryIds(anyLong(), anyList());
    }

    @Test
    void repeatedLikeAndUnlikeChangeCountOnceAndDetailReadsDatabaseState() {
        var mapper = mock(DiaryMapper.class);
        var service = new DiaryService(mapper, null, null, new ObjectMapper());
        var diary = Diary.builder().id(10L).userId(1L).likeCount(1).build();
        when(mapper.lockDiary(10L)).thenReturn(diary);
        when(mapper.selectById(10L)).thenReturn(diary);
        when(mapper.liked(10L, 1L)).thenReturn(0, 1, 1);
        service.likeDiary(1L, 10L);
        service.likeDiary(1L, 10L);
        assertTrue(service.getDiaryInfo(1L, 10L).isLiked());
        verify(mapper, times(1)).addLike(10L, 1L);
        verify(mapper, times(1)).changeLikeCount(10L, 1);
        when(mapper.removeLike(10L, 1L)).thenReturn(1, 0);
        service.unlikeDiary(1L, 10L);
        service.unlikeDiary(1L, 10L);
        verify(mapper, times(1)).changeLikeCount(10L, -1);
    }
}
