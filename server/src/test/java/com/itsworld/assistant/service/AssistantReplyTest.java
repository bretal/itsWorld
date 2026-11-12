package com.itsworld.assistant.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AssistantReplyTest {
    ObjectMapper json=new ObjectMapper();
    AssistantService service=new AssistantService(null,null,null,null,null,json,null);
    @Test void fencedJsonAndStringWrapperRetainAnswer() throws Exception {
        String answer=json.writeValueAsString(java.util.Map.of("answer","洗澡后及时吹干","productIds",java.util.List.of()));
        assertEquals("洗澡后及时吹干",service.parseReply("```json\n"+answer+"\n```").path("answer").asText());
        assertEquals("洗澡后及时吹干",service.parseReply(json.writeValueAsString(answer)).path("answer").asText());
    }
    @Test void missingProductIdsStillRetainsKnowledgeAnswer() throws Exception {
        assertEquals("回答",service.parseReply(json.writeValueAsString(java.util.Map.of("answer","回答"))).path("answer").asText());
    }
    @Test void malformedJsonIsRejected() { assertThrows(java.io.IOException.class,()->service.parseReply("{broken")); }
}
