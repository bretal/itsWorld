package com.itsworld.admin;
import com.itsworld.admin.service.AdminDataService;
import com.itsworld.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/** 管理角色隔离、跨用户查询与内容审核集成回归。 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminManagementIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = createMysql();

    private static MySQLContainer<?> createMysql() {
        MySQLContainer<?> container = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("its_world")
                .withUsername("itsworld")
                .withPassword("itsworld")
                .withCommand("--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci")
                .withInitScript("schema-test.sql");
        return container;
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("jwt.secret", () -> "itsworld-integration-test-secret-key-2026-0123456789-abcdefghijklmnopqrstuvwxyz");
        registry.add("minio.endpoint", () -> "http://localhost:9000");
        registry.add("minio.access-key", () -> "test-access");
        registry.add("minio.secret-key", () -> "test-secret");
        registry.add("app.payment.mock-enabled", () -> "true");
    }


 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate jdbc;
 @Autowired AdminDataService service;
 @Test void rolesProtectEveryManagementArea() throws Exception {
  for(String path:new String[]{"/api/admin/data/pets","/api/admin/data/weight","/api/admin/moderation/reports","/api/admin/member/subscription-plans"}) {
   mvc.perform(get(path)).andExpect(status().isUnauthorized());
   mvc.perform(get(path).with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
   mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
  }
 }
 @Test void allEightResourcesQueryRealSchema() throws Exception {
  for(String kind:new String[]{"pets","diaries","posts","comments","weight","feeding","vaccine","checkup"}) {
   mvc.perform(get("/api/admin/data/"+kind).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.records").isArray());
  }
 }
 @Test void rejectsSqlResourceInjection() {
  assertThrows(BusinessException.class,()->service.list("sys_user",null,null,null,null,null,1,20));
  assertThrows(BusinessException.class,()->service.detail("posts;DROP TABLE sys_user",1));
 }
 @Test void ownerFiltersAndHealthJoinsAreCorrect() {
  jdbc.update("INSERT INTO sys_user(username,password_hash) VALUES('admin_filter_a','test'),('admin_filter_b','test')");
  long a=jdbc.queryForObject("SELECT id FROM sys_user WHERE username='admin_filter_a'",Long.class);
  long b=jdbc.queryForObject("SELECT id FROM sys_user WHERE username='admin_filter_b'",Long.class);
  jdbc.update("INSERT INTO pet_profile(user_id,pet_name,pet_type) VALUES(?,'FilterPet','cat')",a);
  long pet=jdbc.queryForObject("SELECT id FROM pet_profile WHERE user_id=?",Long.class,a);
  jdbc.update("INSERT INTO pet_weight_record(pet_id,weight_kg,recorded_at) VALUES(?,4.2,NOW())",pet);
  assertEquals(1L,service.list("pets",a,null,null,null,null,1,20).getTotal());
  assertEquals(1L,service.list("weight",a,pet,null,null,null,1,20).getTotal());
  assertEquals(0L,service.list("weight",b,null,null,null,null,1,20).getTotal());
  assertEquals(0L,service.list("pets",null,null,null,"' OR 1=1 --",null,1,20).getTotal());
  assertFalse(service.list("pets",a,null,null,null,null,1,20).getRecords().get(0).containsKey("password_hash"));
 }
 @Test void moderationWritesAuditAndMaintainsVisibleCommentCount() {
  jdbc.update("INSERT INTO sys_user(username,password_hash) VALUES('admin_moderate','test')");
  long owner=jdbc.queryForObject("SELECT id FROM sys_user WHERE username='admin_moderate'",Long.class);
  jdbc.update("INSERT INTO community_post(user_id,title,content,status,comment_count) VALUES(?,'title','body',1,1)",owner);
  long id=jdbc.queryForObject("SELECT id FROM community_post WHERE user_id=?",Long.class,owner);
  service.moderate("posts",id,"hide","违规内容",owner);
  assertEquals(0,jdbc.queryForObject("SELECT status FROM community_post WHERE id=?",Integer.class,id));
  service.moderate("posts",id,"publish","复核通过",owner);
  jdbc.update("INSERT INTO community_comment(post_id,user_id,content) VALUES(?,?,'comment')",id,owner);
  long comment=jdbc.queryForObject("SELECT id FROM community_comment WHERE post_id=?",Long.class,id);
  service.moderate("comments",comment,"remove","违规评论",owner);
  assertEquals(0,jdbc.queryForObject("SELECT comment_count FROM community_post WHERE id=?",Integer.class,id));
  assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM admin_moderation_log WHERE admin_id=?",Integer.class,owner));
  assertThrows(BusinessException.class,()->service.moderate("comments",comment,"remove","重复",owner));
 }

 @Test void memberPlanAndRewardConfigurationUsesCurrentContracts() throws Exception {
  long level=jdbc.queryForObject("SELECT MIN(id) FROM member_level",Long.class);
  mvc.perform(post("/api/admin/member/subscription-plans").with(user("admin").roles("ADMIN"))
    .contentType("application/json").content("{\"name\":\"Test plan\",\"levelId\":"+level+",\"price\":100,\"durationDays\":30,\"status\":1}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
  long plan=jdbc.queryForObject("SELECT id FROM member_subscription_plan WHERE name='Test plan'",Long.class);
  mvc.perform(put("/api/admin/member/subscription-plans/"+plan).with(user("admin").roles("ADMIN"))
    .contentType("application/json").content("{\"name\":\"Test plan\",\"levelId\":"+level+",\"price\":200,\"durationDays\":60,\"status\":0}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
  assertEquals(200L,jdbc.queryForObject("SELECT price FROM member_subscription_plan WHERE id=?",Long.class,plan));
  mvc.perform(post("/api/admin/member/reward-products").with(user("admin").roles("ADMIN"))
    .contentType("application/json").content("{\"name\":\"Test reward\",\"growthCost\":100,\"stock\":5,\"status\":1}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
 }
 @Test void redemptionFulfillmentIsIdempotentAndRecordsOperator() throws Exception {
  jdbc.update("INSERT INTO sys_user(username,password_hash,role) VALUES('fulfill_admin','test','ADMIN')");
  long operator=jdbc.queryForObject("SELECT id FROM sys_user WHERE username='fulfill_admin'",Long.class);
  jdbc.update("INSERT INTO member_reward_product(name,growth_cost,stock,status) VALUES('Fulfill reward',100,2,1)");
  long product=jdbc.queryForObject("SELECT id FROM member_reward_product WHERE name='Fulfill reward'",Long.class);
  jdbc.update("INSERT INTO member_redemption(redemption_no,user_id,product_id,name_snapshot,growth_cost,request_key) VALUES('ADMIN-TEST-REDEEM',?,?,'Reward snapshot',100,'admin-test')",operator,product);
  for(int i=0;i<2;i++) {
   mvc.perform(post("/api/admin/member/redemptions/ADMIN-TEST-REDEEM/fulfill").with(user("fulfill_admin").roles("ADMIN")))
    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
  }
  assertEquals("FULFILLED",jdbc.queryForObject("SELECT status FROM member_redemption WHERE redemption_no='ADMIN-TEST-REDEEM'",String.class));
  assertEquals(operator,jdbc.queryForObject("SELECT fulfilled_by FROM member_redemption WHERE redemption_no='ADMIN-TEST-REDEEM'",Long.class));
  assertEquals(2,jdbc.queryForObject("SELECT stock FROM member_reward_product WHERE id=?",Integer.class,product));
 }
 @Test void reportResolutionRecordsConclusionAndRejectsRepeatedProcessing() throws Exception {
  jdbc.update("INSERT INTO sys_user(username,password_hash,role) VALUES('report_admin','test','ADMIN')");
  long owner=jdbc.queryForObject("SELECT id FROM sys_user WHERE username='report_admin'",Long.class);
  jdbc.update("INSERT INTO community_post(user_id,title,content,status) VALUES(?,'Reported','body',1)",owner);
  long target=jdbc.queryForObject("SELECT id FROM community_post WHERE user_id=?",Long.class,owner);
  mvc.perform(post("/api/admin/moderation/reports").with(user("report_admin").roles("ADMIN"))
    .contentType("application/json").content("{\"targetType\":\"posts\",\"targetId\":"+target+",\"reason\":\"External feedback\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
  long report=jdbc.queryForObject("SELECT id FROM content_report WHERE target_id=?",Long.class,target);
  mvc.perform(post("/api/admin/moderation/reports/"+report+"/resolve").with(user("report_admin").roles("ADMIN"))
    .contentType("application/json").content("{\"status\":\"DISMISSED\",\"resolution\":\"Reviewed\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
  mvc.perform(post("/api/admin/moderation/reports/"+report+"/resolve").with(user("report_admin").roles("ADMIN"))
    .contentType("application/json").content("{\"status\":\"RESOLVED\",\"resolution\":\"Repeated\"}"))
    .andExpect(jsonPath("$.code").value(1500));
  assertEquals("DISMISSED",jdbc.queryForObject("SELECT status FROM content_report WHERE id=?",String.class,report));
 }
}
