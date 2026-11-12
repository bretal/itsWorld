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
        }
        jdbc.update("DELETE FROM assistant_turn"); jdbc.update("DELETE FROM assistant_session"); jdbc.update("DELETE FROM assistant_user_quota");
        when(client.model()).thenReturn("test-model");
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
        var actual=new AssistantService(sessionMapper,turns,transactions,productService,actualClient,json,instructions);
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
