package com.itsworld.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.assistant.dto.AssistantDtos.*;
import com.itsworld.assistant.service.*;
import com.itsworld.assistant.mapper.*;
import com.itsworld.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.FileSystemResource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssistantIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("its_world").withUsername("itsworld").withPassword("itsworld")
            .withInitScript("schema-test.sql");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",MYSQL::getJdbcUrl); r.add("spring.datasource.username",MYSQL::getUsername);
        r.add("spring.datasource.password",MYSQL::getPassword);
        r.add("jwt.secret",()->"assistant-integration-test-key-0123456789012345678901234567890123456789");
        r.add("minio.endpoint",()->"http://localhost:9000"); r.add("minio.access-key",()->"test-access"); r.add("minio.secret-key",()->"test-secret");
    }
    @Autowired AssistantService service;
    @Autowired AssistantTurnService transactions;
    @Autowired AssistantProductService productService;
    @Autowired AssistantInstructionService instructions;
    @Autowired AssistantPageTaskService pageTasks;
    @Autowired AssistantSessionMapper sessionMapper;
    @Autowired AssistantTurnMapper turns;
    @Autowired DataSource datasource;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired MockMvc mvc;
    @MockBean DeepSeekClient client;
    @BeforeEach void setup() throws Exception {
        var migration=new FileSystemResource("../init-scripts/mysql/15-assistant.sql");
        if (!migration.exists()) migration=new FileSystemResource("init-scripts/mysql/15-assistant.sql");
        try (var c=datasource.getConnection()) {
            ScriptUtils.executeSqlScript(c,migration); ScriptUtils.executeSqlScript(c,migration);
            ScriptUtils.executeSqlScript(c,new FileSystemResource(migration.getFile().toPath().resolveSibling("17-assistant-instructions.sql")));
            c.createStatement().execute("DROP TABLE IF EXISTS assistant_page_task");
            ScriptUtils.executeSqlScript(c, new org.springframework.core.io.ClassPathResource("db/migration/V19__assistant_page_tasks.sql"));
        }
        jdbc.update("DELETE FROM assistant_turn"); jdbc.update("DELETE FROM assistant_session"); jdbc.update("DELETE FROM assistant_user_quota");
        when(client.model()).thenReturn("test-model");
    }
    private com.fasterxml.jackson.databind.JsonNode pageTool(String id, String name, String args) {
        var response = json.createObjectNode();
        var message = response.putArray("choices").addObject().putObject("message").put("role", "assistant");
        message.putArray("tool_calls").addObject().put("id", id).put("type", "function")
                .putObject("function").put("name", name).put("arguments", args);
        return response;
    }

    @Test void pageTaskResultReplayOwnershipAndCancellation() {
        long session = Long.parseLong(service.create(1L).id());
        when(client.complete(any(), any(), anyLong())).thenReturn(
                pageTool("page_nav", "navigate", "{\"page\":\"health\"}"),
                pageTool("page_ask", "ask_user", "{\"question\":\"请提供体重\"}"));
        var request = new AssistantPageTaskService.CreateRequest("page_request_001", session, "记录体重", null);
        var task = pageTasks.create(1L, request);
        String id = task.path("id").asText();
        assertEquals(id, pageTasks.create(1L, request).path("id").asText());
        assertThrows(BusinessException.class, () -> pageTasks.get(2L, id));
        var result = new AssistantPageTaskService.StepRequest("page_result_001", task.path("action").path("id").asText(), json.createObjectNode().put("ok", true), null);
        assertEquals("ASK_USER", pageTasks.advance(1L, id, result).path("status").asText());
        assertEquals("ASK_USER", pageTasks.advance(1L, id, result).path("status").asText());
        verify(client, times(2)).complete(any(), any(), anyLong());
        assertEquals("CANCELLED", pageTasks.cancel(1L, id).path("status").asText());
        assertEquals("CANCELLED", pageTasks.advance(1L, id, result).path("status").asText());
    }

    @Test void pageWeightSaveIsIdempotentUnderConcurrentRetries() throws Exception {
        jdbc.update("INSERT INTO sys_user(id,username,password_hash,status) VALUES(887766,'page_task_user','test',1)");
        jdbc.update("INSERT INTO pet_profile(id,user_id,pet_name,pet_type) VALUES(887766,887766,'测试猫','cat')");
        try {
            long session = Long.parseLong(service.create(887766L).id());
            when(client.complete(any(), any(), anyLong())).thenReturn(pageTool("page_save", "submit_record",
                    "{\"petId\":\"887766\",\"weightKg\":4.2,\"date\":\"2026-11-23\",\"revision\":3}"));
            var task = pageTasks.create(887766L, new AssistantPageTaskService.CreateRequest(
                    "page_save_request", session, "记录测试猫体重4.2公斤", 887766L));
            String id = task.path("id").asText();
            String actionId = task.path("action").path("id").asText();
            var request = com.itsworld.health.dto.WeightRecordCreateRequest.builder().petId(887766L)
                    .weightKg(new java.math.BigDecimal("4.20"))
                    .recordedAt(java.time.LocalDateTime.of(2026, 11, 23, 0, 0)).build();
            assertThrows(BusinessException.class, () -> pageTasks.saveWeight(887766L, id, "wrong_action", request));
            var executor = Executors.newFixedThreadPool(2);
            try {
                var first = executor.submit(() -> pageTasks.saveWeight(887766L, id, actionId, request));
                var second = executor.submit(() -> pageTasks.saveWeight(887766L, id, actionId, request));
                assertEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            } finally { executor.shutdownNow(); }
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM pet_weight_record WHERE pet_id=887766", Integer.class));
            assertEquals("COMPLETED", pageTasks.get(887766L, id).path("status").asText());
        } finally {
            jdbc.update("DELETE FROM pet_weight_record WHERE pet_id=887766");
            jdbc.update("DELETE FROM pet_profile WHERE id=887766");
            jdbc.update("DELETE FROM sys_user WHERE id=887766");
        }
    }

    @Test void pageTaskNormalizesParallelCallsAndPlainTextQuestions() {
        long session = Long.parseLong(service.create(1L).id());
        var plain = json.createObjectNode();
        plain.putArray("choices").addObject().putObject("message").put("role", "assistant").put("content", "体重是多少？");
        var parallel = pageTool("parallel_first", "navigate", "{\"page\":\"health\"}");
        var calls = (com.fasterxml.jackson.databind.node.ArrayNode) parallel.path("choices").path(0).path("message").path("tool_calls");
        calls.add(calls.get(0).deepCopy());
        when(client.complete(any(), any(), anyLong())).thenReturn(plain, parallel,
                pageTool("parallel_ask", "ask_user", "{\"question\":\"体重是多少？\"}"), plain, plain);
        var task = pageTasks.create(1L, new AssistantPageTaskService.CreateRequest("parallel_request_001", session, "记录体重", null));
        assertEquals("navigate", task.path("action").path("name").asText());
        var next = pageTasks.advance(1L, task.path("id").asText(), new AssistantPageTaskService.StepRequest(
                "parallel_result_001", task.path("action").path("id").asText(), json.createObjectNode().put("ok", true), null));
        assertEquals("ASK_USER", next.path("status").asText());
        var fallback = pageTasks.create(1L, new AssistantPageTaskService.CreateRequest("plaintext_request_001", session, "记录体重", null));
        assertEquals("ASK_USER", fallback.path("status").asText());
    }
    @Test void authenticationAndOwnership() throws Exception {
        mvc.perform(get("/api/assistant/sessions")).andExpect(status().isUnauthorized());
        long id=Long.parseLong(service.create(1L).id());
        assertThrows(BusinessException.class,()->service.history(2L,id,null));
        assertThrows(BusinessException.class,()->service.send(2L,id,new SendRequest("request_123","test")));
        assertTrue(service.sessions(2L).isEmpty()); verify(client,never()).complete(any(),any(),anyLong());
    }
    @Test void replayConflictAndSuccessfulContext() throws Exception {
        long id=Long.parseLong(service.create(1L).id());
        var reply=json.createObjectNode();
        reply.putArray("choices").addObject().putObject("message").put("role","assistant")
                .put("content",json.writeValueAsString(Map.of("answer","多梳毛并观察皮肤状态","productIds",List.of())));
        when(client.complete(any(),any(),anyLong())).thenReturn(reply);
        var request=new SendRequest("request_123","猫为什么掉毛");
        var result=service.send(1L,id,request); assertEquals("SUCCEEDED",result.status()); assertTrue(result.products().isEmpty());
        assertEquals(result,service.send(1L,id,request)); verify(client,times(1)).complete(any(),any(),anyLong());
        assertThrows(BusinessException.class,()->service.send(1L,id,new SendRequest("request_123","换问题")));
        long other=Long.parseLong(service.create(1L).id());
        assertThrows(BusinessException.class,()->service.send(1L,other,request));
        assertEquals(1,service.history(1L,id,null).records().size());
    }
    @Test void failureRetryAndLeaseTakeover() {
        long id=Long.parseLong(service.create(1L).id());
        var first=transactions.acquire(1L,id,"request_123","问题","test");
        assertThrows(BusinessException.class,()->transactions.acquire(1L,id,"request_456","问题2","test"));
        jdbc.update("UPDATE assistant_turn SET lease_until=DATE_SUB(NOW(),INTERVAL 1 SECOND)");
        jdbc.update("UPDATE assistant_user_quota SET lease_until=DATE_SUB(NOW(),INTERVAL 1 SECOND)");
        var newer=transactions.acquire(1L,id,"request_123","问题","test");
        assertEquals(2,newer.getAttemptCount());
        assertThrows(BusinessException.class,()->transactions.finish(first,"旧结果","[]",0,0));
        transactions.fail(first,"OLD"); assertEquals("PENDING",turns.selectById(newer.getId()).getStatus());
        transactions.fail(newer,"ASSISTANT_TIMEOUT");
        var retry=transactions.acquire(1L,id,"request_123","问题","test"); assertEquals(3,retry.getAttemptCount());
        transactions.finish(retry,"成功","[]",1,1);
        assertEquals("SUCCEEDED",turns.selectById(retry.getId()).getStatus());
    }
    @Test void quotasAndHourResetAndDailySessionLimit() {
        long id=Long.parseLong(service.create(1L).id());
        for (int i=0;i<20;i++) {
            var t=transactions.acquire(1L,id,"request_"+i,"问题","test"); transactions.fail(t,"ASSISTANT_TIMEOUT");
        }
        assertThrows(BusinessException.class,()->transactions.acquire(1L,id,"request_extra","问题","test"));
        jdbc.update("UPDATE assistant_user_quota SET hour_start=DATE_SUB(hour_start,INTERVAL 1 HOUR)");
        var reset=transactions.acquire(1L,id,"request_extra","问题","test"); transactions.fail(reset,"ASSISTANT_TIMEOUT");
        for (int i=1;i<30;i++) service.create(1L);
        assertThrows(BusinessException.class,()->service.create(1L));
    }
    @Test void concurrentDifferentSessionsAndIndependentUsers() throws Exception {
        long a=Long.parseLong(service.create(1L).id()), b=Long.parseLong(service.create(1L).id());
        var pool=Executors.newFixedThreadPool(2); var gate=new CountDownLatch(1);
        try {
            List<Future<Boolean>> results=new ArrayList<>();
            for (long id:List.of(a,b)) results.add(pool.submit(()->{ gate.await(); try {
                transactions.acquire(1L,id,"request_"+id,"问题","test"); return true;
            } catch (BusinessException e) { return false; } }));
            gate.countDown(); int successes=0; for (var f:results) if (f.get(15,TimeUnit.SECONDS)) successes++;
            assertEquals(1,successes);
            long other=Long.parseLong(service.create(2L).id());
            assertNotNull(transactions.acquire(2L,other,"request_other","问题","test"));
        } finally { pool.shutdownNow(); }
    }
    @Test void historyPaginationKeepsWholeTurnsAndExpiredPendingCanRetry() {
        long id=Long.parseLong(service.create(1L).id());
        for (int i=0;i<20;i++) {
            var t=transactions.acquire(1L,id,"request_"+i,"问题","test"); transactions.finish(t,"回答","[]",0,0);
        }
        jdbc.update("UPDATE assistant_user_quota SET hour_start=DATE_SUB(hour_start,INTERVAL 1 HOUR)");
        var pending=transactions.acquire(1L,id,"request_pending","未完成","test");
        jdbc.update("UPDATE assistant_turn SET lease_until=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE id=?",pending.getId());
        var page=service.history(1L,id,null); assertTrue(page.hasMore()); assertEquals(20,page.records().size());
        assertEquals("FAILED",page.records().get(19).status());
        assertEquals(1,service.history(1L,id,Long.valueOf(page.nextBeforeId())).records().size());
    }
    @Test void realCatalogToolRoundTripAndFinalWhitelist() throws Exception {
        jdbc.update("INSERT INTO shop_category(id,name,status) VALUES(990001,'助手测试分类','ENABLED') ON DUPLICATE KEY UPDATE status='ENABLED'");
        jdbc.update("INSERT INTO shop_product(id,product_code,category_id,name,sale_status) VALUES(990001,'ASSISTANT_TEST',990001,'助手测试冻干','ON_SALE') ON DUPLICATE KEY UPDATE sale_status='ON_SALE'");
        jdbc.update("INSERT INTO shop_sku(id,product_id,sku_code,sale_price,status) VALUES(990001,990001,'ASSISTANT_SKU',3990,'ENABLED') ON DUPLICATE KEY UPDATE status='ENABLED'");
        jdbc.update("INSERT INTO shop_inventory(sku_id,warehouse_id,available_qty) VALUES(990001,0,12) ON DUPLICATE KEY UPDATE available_qty=12");
        long id=Long.parseLong(service.create(999999L).id());
        var toolReply=json.createObjectNode();
        toolReply.putArray("choices").addObject().putObject("message").put("role","assistant")
                .putArray("tool_calls").addObject().put("id","call_1").put("type","function")
                .putObject("function").put("name","search_products").put("arguments",json.writeValueAsString(Map.of("keyword","助手测试冻干","maxPriceYuan",40)));
        var finalReply=json.createObjectNode();
        finalReply.putArray("choices").addObject().putObject("message").put("role","assistant")
                .put("content",json.writeValueAsString(Map.of("answer","查询到了相关商品","productIds",List.of("990001"))));
        when(client.complete(any(),any(),anyLong())).thenReturn(toolReply,finalReply);
        var result=service.send(999999L,id,new SendRequest("request_catalog","40元内的冻干"));
        assertEquals(1,result.products().size()); assertEquals(3990,result.products().get(0).priceCents());
        assertEquals(12,result.products().get(0).availableQty()); assertEquals("990001",result.products().get(0).id());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM member_account WHERE user_id=999999",Integer.class));
        verify(client,times(2)).complete(any(),any(),anyLong());
    }
    @Test void invalidToolEmptyReplyAndTimeoutFailDurably() throws Exception {
        long id=Long.parseLong(service.create(1L).id());
        var empty=json.createObjectNode(); empty.putArray("choices").addObject().putObject("message").put("content","");
        when(client.complete(any(),any(),anyLong())).thenReturn(empty);
        assertThrows(BusinessException.class,()->service.send(1L,id,new SendRequest("request_empty","test")));
        assertEquals("FAILED",service.history(1L,id,null).records().get(0).status());
        when(client.complete(any(),any(),anyLong())).thenThrow(new BusinessException("ASSISTANT_TIMEOUT"));
        assertThrows(BusinessException.class,()->service.send(1L,id,new SendRequest("request_empty","test")));
        assertEquals(2,jdbc.queryForObject("SELECT attempt_count FROM assistant_turn WHERE request_id='request_empty'",Integer.class));
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="assistant.liveSmoke",matches="true")
    void liveSmokeWithTemporaryCatalog() {
        String key=System.getenv("DEEPSEEK_API_KEY");
        assertNotNull(key,"DEEPSEEK_API_KEY must be set locally");
        jdbc.update("INSERT INTO shop_category(id,name,status) VALUES(990002,'测试用品','ENABLED')");
        jdbc.update("INSERT INTO shop_product(id,product_code,category_id,name,sale_status) VALUES(990002,'SMOKE_TEST',990002,'测试冻干零食','ON_SALE')");
        jdbc.update("INSERT INTO shop_sku(id,product_id,sku_code,sale_price,status) VALUES(990002,990002,'SMOKE_SKU',3990,'ENABLED')");
        jdbc.update("INSERT INTO shop_inventory(sku_id,warehouse_id,available_qty) VALUES(990002,0,12)");
        var actualClient=new DeepSeekClient(json,true,key,"https://api.deepseek.com","deepseek-flash");
        var actual=new AssistantService(sessionMapper,turns,transactions,productService,actualClient,json,instructions,null,null);
        long id=Long.parseLong(actual.create(999998L).id());
        var result=actual.send(999998L,id,new SendRequest("live_smoke_123","请查询商城100元以内的测试冻干零食，只看当前在售有货的商品，不需要适龄或医疗建议。"));
        assertEquals("SUCCEEDED",result.status()); assertFalse(result.content().isBlank());
        assertFalse(result.products().isEmpty(),"Expected a tool-backed product card");
        assertEquals("990002",result.products().get(0).id());
        assertEquals(3990,result.products().get(0).priceCents());
    }

    @Test void draftDoesNotApplyUntilPublishAndRollbackRestoresDefaults() throws Exception {
        jdbc.update("DELETE FROM assistant_instruction_config");
        var pet = new com.itsworld.pet.entity.Pet();
        pet.setPetType("cat");
        pet.setBreed("测试品种");
        pet.setBirthDate(java.time.LocalDate.now().minusMonths(7).toString());
        pet.setDescription("PRIVATE_PROFILE_DESCRIPTION");
        var defaults = instructions.settings().published();
        var config = new AssistantInstructionService.Config("TEST_INSTRUCTION", java.util.Map.of(
                AssistantInstructionService.Scene.SHOP, java.util.Map.of(
                        AssistantInstructionService.Field.BREED, AssistantInstructionService.Missing.ASK),
                AssistantInstructionService.Scene.CARE, java.util.Map.of(),
                AssistantInstructionService.Scene.HEALTH, java.util.Map.of()));
        try {
            assertEquals(defaults, instructions.saveDraft(config).published());
            assertTrue(instructions.systemPrompt(pet, "推荐零食", List.of()).contains("7个月"));
            assertEquals(1, instructions.publish().revision());
            String prompt = instructions.systemPrompt(pet, "猫，零食", List.of("推荐100元内的商品"));
            assertTrue(prompt.contains("TEST_INSTRUCTION"));
            assertTrue(prompt.contains("测试品种"));
            assertFalse(prompt.contains("7个月"));
            assertFalse(prompt.contains("PRIVATE_PROFILE_DESCRIPTION"));
            assertEquals(defaults, instructions.rollback().published());
        } finally {
            jdbc.update("DELETE FROM assistant_instruction_config");
        }
    }
}
