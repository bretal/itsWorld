package com.itsworld.assistant.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.assistant.dto.AssistantDtos.AlbumDraft;
import com.itsworld.assistant.entity.AssistantTurn;
import com.itsworld.assistant.mapper.AssistantTurnMapper;
import com.itsworld.diary.service.DiaryService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
class AssistantReplyTest {
    ObjectMapper json=new ObjectMapper();
    AssistantService service=new AssistantService(null,null,null,null,null,json,null,null,null);
    @Test void fencedJsonAndStringWrapperRetainAnswer() throws Exception {
        String answer=json.writeValueAsString(java.util.Map.of("answer","洗澡后及时吹干","productIds",java.util.List.of()));
        assertEquals("洗澡后及时吹干",service.parseReply("```json\n"+answer+"\n```").path("answer").asText());
        assertEquals("洗澡后及时吹干",service.parseReply(json.writeValueAsString(answer)).path("answer").asText());
    }
    @Test void missingProductIdsStillRetainsKnowledgeAnswer() throws Exception {
        assertEquals("回答",service.parseReply(json.writeValueAsString(java.util.Map.of("answer","回答"))).path("answer").asText());
    }
    @Test void malformedJsonIsRejected() { assertThrows(java.io.IOException.class,()->service.parseReply("{broken")); }

    @Test void albumConfirmationReusesSavedDiaryOnRetry() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var turns = mock(AssistantTurnMapper.class);
        var diaries = mock(DiaryService.class);
        var assistant = new AssistantService(null, turns, null, null, null, mapper, null, diaries, null);
        var draft = new AlbumDraft("公园散步", "带金喜去公园", "公园",
                LocalDateTime.of(2026, 11, 18, 17, 40), List.of(), 3L, null, "calm", "daily", 1);
        var turn = new AssistantTurn();
        turn.setId(10L);
        turn.setSessionId(20L);
        turn.setStatus("SUCCEEDED");
        var payload = mapper.createObjectNode();
        payload.putArray("products");
        payload.set("draft", mapper.valueToTree(draft));
        turn.setProductsJson(mapper.writeValueAsString(payload));
        when(turns.selectOne(any())).thenReturn(turn);
        when(turns.updateById(turn)).thenReturn(1);
        when(diaries.createDiary(eq(1L), any())).thenReturn(30L);

        var saved = assistant.saveAlbum(1L, 20L, 10L, draft);
        assertEquals("30", saved.diaryId());
        assertEquals(saved, assistant.saveAlbum(1L, 20L, 10L, draft));
        verify(diaries, times(1)).createDiary(eq(1L), argThat(input ->
                input.getIsPublic() == 1 && input.getDiaryDate().equals("2026-11-18")
                        && input.getLocation().equals("公园")
                        && input.getMood().equals("calm") && input.getCategory().equals("daily")
                        && input.getContent().equals("带金喜去公园")
                        && input.getOccurredAt().equals(draft.occurredAt())));
    }
}
