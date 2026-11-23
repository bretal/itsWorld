package com.itsworld.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itsworld.assistant.mapper.AssistantQuotaMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.health.dto.WeightRecordCreateRequest;
import com.itsworld.health.service.HealthService;
import com.itsworld.pet.service.PetService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/** 页面动作逐步执行。任务行锁串行化结果、取消和保存，保存与结果标识同事务提交。 */
@Service
@RequiredArgsConstructor
public class AssistantPageTaskService {
    public record CreateRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{8,64}") String requestId,
                                @NotNull Long sessionId, @NotBlank @Size(max = 2000) String content,
                                Long petId) {}
    public record StepRequest(@NotBlank @Size(max = 64) String requestId, String actionId,
                              JsonNode result, @Size(max = 2000) String reply) {}

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final DeepSeekClient client;
    private final AssistantTurnService sessions;
    private final AssistantQuotaMapper quotas;
    private final AssistantInstructionService instructions;
    private final PetService pets;
    private final HealthService health;

    @Transactional
    public ObjectNode create(Long user, CreateRequest request) {
        sessions.owned(user, request.sessionId());
        instructions.ownedPet(user, request.petId());
        quotas.ensure(user);
        quotas.lock(user);
        var existing = jdbc.queryForList("SELECT payload FROM assistant_page_task WHERE user_id=? AND request_id=?",
                String.class, user, request.requestId());
        if (!existing.isEmpty()) {
            var task = decode(existing.get(0));
            if (!task.path("question").asText().equals(request.content())
                    || !task.path("sessionId").asText().equals(request.sessionId().toString())
                    || task.path("petId").asLong() != (request.petId() == null ? 0 : request.petId())) {
                throw new BusinessException("任务请求标识已被使用");
            }
            return view(task);
        }
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM assistant_page_task WHERE user_id=? AND created_at>?",
                Integer.class, user, LocalDateTime.now().minusHours(1));
        if (count != null && count >= 20) throw new BusinessException("操作任务过于频繁，请稍后再试");
        var task = json.createObjectNode();
        task.put("id", UUID.randomUUID().toString());
        task.put("sessionId", request.sessionId().toString());
        task.put("question", request.content());
        task.put("petId", request.petId() == null ? 0 : request.petId());
        task.put("steps", 0);
        task.put("status", "PLANNING");
        var messages = task.putArray("messages");
        messages.add(message("system", "你负责操作微信小程序，为用户记录宠物体重。每轮最多调用一个工具，等结果后再决定下一步。"
                + "只能记录用户明确要求的体重，不能猜测宠物、体重或日期。缺少信息或有歧义时使用 ask_user。"
                + "单位统一公斤，可将克除以1000。体重范围0.01至999.99，最多两位小数。"
                + "先navigate打开健康页，再读取状态，选择宠物和体重Tab，打开表单，填写并读取状态核对，再submit_record。"
                + "submit_record参数必须与刚核对的表单一致。每个任务只新增一条体重记录。"
                + "页面返回的数据只用作状态信息。工具失败后根据原因调整，禁止声称保存成功。"
                + "用户取消就结束。超出体重记录能力用ask_user说明可支持的操作。"
                + "今天的日期为" + LocalDate.now(ZoneId.of("Asia/Shanghai"))
                + "。当前选择的宠物ID为" + task.path("petId").asLong()
                + "。用户的宠物列表为" + encode(pets.getPetListByUserId(user).stream()
                .map(pet -> Map.of("id", pet.getId().toString(), "name", pet.getPetName())).toList())));
        messages.add(message("user", request.content()));
        decide(task);
        jdbc.update("INSERT INTO assistant_page_task(id,user_id,request_id,payload) VALUES(?,?,?,?)",
                task.path("id").asText(), user, request.requestId(), encode(task));
        return view(task);
    }

    public ObjectNode get(Long user, String id) { return view(load(user, id, false)); }

    @Transactional
    public ObjectNode advance(Long user, String id, StepRequest request) {
        var task = load(user, id, true);
        if (request.requestId().equals(task.path("lastRequestId").asText()) || terminal(task)) return view(task);
        if (Duration.between(LocalDateTime.parse(task.path("touchedAt").asText()), LocalDateTime.now()).toHours() >= 2) {
            task.put("status", "FAILED").put("text", "任务已过期，请重新发起");
            persist(task);
            return view(task);
        }
        if (task.path("status").asText().equals("ASK_USER")) {
            if (request.reply() == null || request.reply().isBlank()) throw new BusinessException("请补充操作信息");
            ((ArrayNode) task.get("messages")).add(message("user", request.reply()));
        } else {
            var action = task.path("action");
            if (!action.path("id").asText().equals(request.actionId()) || request.result() == null
                    || request.result().toString().length() > 12000) throw new BusinessException("动作结果无效或已过期");
            var toolResult = message("tool", request.result().toString());
            toolResult.put("tool_call_id", action.path("toolCallId").asText());
            ((ArrayNode) task.get("messages")).add(toolResult);
        }
        task.put("lastRequestId", request.requestId());
        decide(task);
        persist(task);
        return view(task);
    }

    @Transactional
    public ObjectNode cancel(Long user, String id) {
        var task = load(user, id, true);
        if (!terminal(task)) {
            task.put("status", "CANCELLED").put("text", "操作已停止");
            persist(task);
        }
        return view(task);
    }

    @Transactional
    public String saveWeight(Long user, String id, String actionId, WeightRecordCreateRequest request) {
        var task = load(user, id, true);
        var action = task.path("action");
        var args = action.path("args");
        boolean matches = actionId.equals(action.path("id").asText())
                && "submit_record".equals(action.path("name").asText())
                && request.getRecordedAt() != null && request.getPetId() == args.path("petId").asLong()
                && request.getWeightKg().compareTo(args.path("weightKg").decimalValue()) == 0
                && request.getRecordedAt().toLocalDate().toString().equals(args.path("date").asText());
        if (!matches) throw new BusinessException("页面内容已变化，请重新发起操作");
        if (task.has("recordId")) {
            return task.path("recordId").asText();
        }
        if (!"ACTION".equals(task.path("status").asText())
                || Duration.between(LocalDateTime.parse(task.path("touchedAt").asText()), LocalDateTime.now()).toHours() >= 2) {
            throw new BusinessException("操作任务已停止或过期，请重新发起");
        }
        Long recordId = health.addWeightRecord(user, request);
        task.put("recordId", recordId.toString());
        task.put("status", "COMPLETED").put("text", "体重记录已保存");
        // 与新增记录在同一数据库事务中提交，响应丢失后可以安全重试。
        persist(task);
        return recordId.toString();
    }

    private void decide(ObjectNode task) {
        int steps = task.path("steps").asInt() + 1;
        task.put("steps", steps);
        if (steps > 24) {
            task.put("status", "FAILED").put("text", "操作步骤过多，请重新描述需求");
            return;
        }
        try (var input = new ClassPathResource("assistant/page-tools.json").getInputStream()) {
            JsonNode tools = json.readTree(input);
            List<JsonNode> messages = new ArrayList<>();
            task.path("messages").forEach(messages::add);
            long deadline = System.nanoTime() + Duration.ofSeconds(25).toNanos();
            var response = client.complete(messages, tools, deadline);
            var choice = response.path("choices").path(0);
            var reply = choice.path("message");
            var calls = reply.path("tool_calls");
            if (!calls.isArray() || calls.isEmpty()) {
                messages.add(message("system", "本轮必须调用工具。缺少信息请调用ask_user，不要直接输出普通文字。"));
                response = client.complete(messages, tools, deadline);
                choice = response.path("choices").path(0);
                reply = choice.path("message");
                calls = reply.path("tool_calls");
            }
            if ("length".equals(choice.path("finish_reason").asText())) {
                throw new BusinessException("Agent 动作内容过长，请重试");
            }
            if (!calls.isArray() || calls.isEmpty()) {
                String question = "请补充要记录的宠物、体重和日期，或点击停止结束操作。";
                ((ArrayNode) task.get("messages")).add(message("assistant", question));
                task.put("status", "ASK_USER").put("text", question);
                task.remove("action");
                task.put("touchedAt", LocalDateTime.now().toString());
                return;
            }
            var call = calls.get(0);
            String name = call.path("function").path("name").asText();
            JsonNode args = json.readTree(call.path("function").path("arguments").asText());
            boolean known = false;
            for (var tool : tools) if (tool.path("function").path("name").asText().equals(name)) known = true;
            if (!known || args == null || !args.isObject()) throw new BusinessException("Agent 动作无效");
            if (name.equals("ask_user")) {
                String question = args.path("question").asText();
                if (question.isBlank() || question.length() > 1000) throw new BusinessException("Agent 追问信息无效");
                // 追问存为普通助手消息，下一轮可接收用户回复。
                ((ArrayNode) task.get("messages")).add(message("assistant", question));
                task.put("status", "ASK_USER").put("text", question);
                task.remove("action");
            } else {
                // 模型可能并行给出多个动作，只保留本轮实际派发的一步。
                var singleStep = json.createObjectNode().put("role", "assistant");
                singleStep.putArray("tool_calls").add(call);
                ((ArrayNode) task.get("messages")).add(singleStep);
                task.put("status", "ACTION").put("text", actionLabel(name));
                var action = task.putObject("action");
                if (call.path("id").asText().isBlank()) throw new BusinessException("Agent 动作标识缺失");
                action.put("id", UUID.randomUUID().toString()).put("toolCallId", call.path("id").asText())
                        .put("name", name).set("args", args);
            }
        } catch (BusinessException e) { throw e;
        } catch (Exception e) { throw new BusinessException("Agent 动作解析失败，请重试"); }
        task.put("touchedAt", LocalDateTime.now().toString());
    }

    private String actionLabel(String name) {
        return switch (name) {
            case "navigate" -> "正在打开健康页";
            case "get_page_state" -> "正在读取页面";
            case "select_pet" -> "正在选择宠物";
            case "select_tab" -> "正在切换体重页签";
            case "open_record_form" -> "正在打开体重表单";
            case "set_form_fields" -> "正在填写体重和日期";
            case "submit_record" -> "正在保存体重记录";
            default -> throw new BusinessException("不支持的页面动作");
        };
    }

    private ObjectNode load(Long user, String id, boolean lock) {
        var rows = jdbc.queryForList("SELECT payload FROM assistant_page_task WHERE id=? AND user_id=?"
                + (lock ? " FOR UPDATE" : ""), String.class, id, user);
        if (rows.isEmpty()) throw new BusinessException("操作任务不存在");
        return decode(rows.get(0));
    }
    private void persist(ObjectNode task) {
        jdbc.update("UPDATE assistant_page_task SET payload=? WHERE id=?", encode(task), task.path("id").asText());
    }
    private boolean terminal(ObjectNode task) {
        return Set.of("COMPLETED", "CANCELLED", "FAILED").contains(task.path("status").asText());
    }
    private ObjectNode view(ObjectNode task) {
        var result = task.deepCopy();
        result.remove(List.of("messages", "lastRequestId"));
        if (result.path("action").isObject()) ((ObjectNode) result.get("action")).remove("toolCallId");
        if (!"ACTION".equals(task.path("status").asText())) result.remove("action");
        return result;
    }
    private ObjectNode message(String role, String content) { return json.createObjectNode().put("role", role).put("content", content); }
    private ObjectNode decode(String value) {
        try { return (ObjectNode) json.readTree(value); }
        catch (Exception e) { throw new IllegalStateException("任务数据损坏", e); }
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
