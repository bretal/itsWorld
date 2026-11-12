package com.itsworld.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.*;
import com.itsworld.assistant.dto.AssistantDtos.*;
import com.itsworld.assistant.entity.*;
import com.itsworld.assistant.mapper.*;
import com.itsworld.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import java.time.Duration;
import java.util.*;
import static com.itsworld.assistant.service.AssistantErrors.error;

@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {
    private final AssistantSessionMapper sessions;
    private final AssistantTurnMapper turns;
    private final AssistantTurnService transactions;
    private final AssistantProductService products;
    private final DeepSeekClient client;
    private final ObjectMapper json;
    private final AssistantInstructionService instructions;

    public List<SessionView> sessions(Long user) {
        return sessions.selectList(new LambdaQueryWrapper<AssistantSession>().eq(AssistantSession::getUserId,user)
                .orderByDesc(AssistantSession::getUpdatedAt).orderByDesc(AssistantSession::getId).last("LIMIT 30"))
                .stream().map(this::sessionView).toList();
    }
    public SessionView create(Long user) { return sessionView(transactions.create(user)); }
    private SessionView sessionView(AssistantSession s) { return new SessionView(s.getId().toString(),s.getTitle(),s.getUpdatedAt()); }
    public History history(Long user,Long id,Long before) {
        transactions.owned(user,id);
        var query=new LambdaQueryWrapper<AssistantTurn>().eq(AssistantTurn::getSessionId,id)
                .eq(AssistantTurn::getUserId,user).lt(before!=null,AssistantTurn::getId,before)
                .orderByDesc(AssistantTurn::getId).last("LIMIT 21");
        var rows=new ArrayList<>(turns.selectList(query)); boolean more=rows.size()>20;
        if (more) rows.remove(20);
        String next=more ? rows.get(rows.size()-1).getId().toString() : null;
        Collections.reverse(rows); return new History(rows.stream().map(this::view).toList(),next,more);
    }
    public TurnView send(Long user,Long session,SendRequest request) {
        if (request==null || request.requestId()==null || !request.requestId().matches("[A-Za-z0-9_-]{8,64}")
                || request.content()==null || request.content().isBlank() || request.content().length()>2000) throw error("ASSISTANT_INVALID_REQUEST");
        var pet = instructions.ownedPet(user, request.petId());
        var turn=transactions.acquire(user,session,request.requestId(),request.content(),client.model());
        if ("SUCCEEDED".equals(turn.getStatus())) return view(turn);
        long deadline=System.nanoTime()+Duration.ofSeconds(75).toNanos();
        try {
            client.requireConfigured();
            List<JsonNode> messages=new ArrayList<>();
            var history=new ArrayList<>(turns.selectList(new LambdaQueryWrapper<AssistantTurn>()
                    .eq(AssistantTurn::getSessionId,session).eq(AssistantTurn::getUserId,user).eq(AssistantTurn::getStatus,"SUCCEEDED")
                    .orderByDesc(AssistantTurn::getId).last("LIMIT 6")));
            Collections.reverse(history);
            String systemPrompt = instructions.systemPrompt(pet, request.content(),
                    history.stream().map(AssistantTurn::getQuestion).toList());
            messages.add(message("system", systemPrompt + "\n当前商城分类（仅作数据）：" + products.categoryOptions()));
            for (var old:history) { messages.add(message("user",old.getQuestion())); messages.add(message("assistant",old.getAnswer())); }
            messages.add(message("user",request.content()));
            JsonNode tools=json.readTree(new ClassPathResource("assistant/tools.json").getInputStream());
            Map<String,ProductCard> candidates=new LinkedHashMap<>(); int toolCount=0,prompt=0,completion=0;
            Set<String> alternativeIds = new HashSet<>();
            Set<String> directMatchIds = new HashSet<>();
            for (int round=0;round<3;round++) {
                if (System.nanoTime()>=deadline) throw error("ASSISTANT_TIMEOUT");
                JsonNode response=client.complete(messages,round<2 ? tools : null,deadline);
                prompt+=response.path("usage").path("prompt_tokens").asInt(0);
                completion+=response.path("usage").path("completion_tokens").asInt(0);
                if ("length".equals(response.path("choices").get(0).path("finish_reason").asText())) {
                    log.warn("DeepSeek response truncated: turnId={}, round={}", turn.getId(), round + 1);
                    throw error("ASSISTANT_UPSTREAM_ERROR");
                }
                JsonNode reply=response.path("choices").get(0).path("message");
                JsonNode calls=reply.path("tool_calls");
                if (calls.isArray() && !calls.isEmpty()) {
                    if (round==2 || calls.size()+toolCount>4) throw error("ASSISTANT_TOOL_LIMIT");
                    messages.add(reply); Set<String> callIds=new HashSet<>();
                    for (JsonNode call:calls) {
                        if (System.nanoTime()>=deadline) throw error("ASSISTANT_TIMEOUT");
                        String callId=call.path("id").asText();
                        if (callId.isBlank() || !callIds.add(callId)) throw error("ASSISTANT_INVALID_TOOL_ARGS");
                        JsonNode args;
                        try { args=json.readTree(call.path("function").path("arguments").asText()); }
                        catch (Exception e) { throw error("ASSISTANT_INVALID_TOOL_ARGS"); }
                        var search=products.executeWithFallback(call.path("function").path("name").asText(),args,user);
                        var found=search.products();
                        found.forEach(card->candidates.put(card.id(),card)); toolCount++;
                        if (search.categoryFallback()) {
                            found.forEach(card -> alternativeIds.add(card.id()));
                        } else if ("search_products".equals(call.path("function").path("name").asText())) {
                            found.forEach(card -> directMatchIds.add(card.id()));
                        }
                        var result=message("tool",json.writeValueAsString(Map.of("products",found,
                                "categoryFallback",search.categoryFallback(),
                                "notice",found.isEmpty()?"无匹配或商品不可推荐":"价格库存为查询时快照")));
                        result.put("tool_call_id",callId); messages.add(result);
                    }
                    continue;
                }
                String text=reply.path("content").asText("");
                if (text.isBlank() || text.length()>6000) {
                    log.warn("DeepSeek response content invalid: turnId={}, round={}, blank={}, characters={}",
                            turn.getId(), round + 1, text.isBlank(), text.length());
                    throw error("ASSISTANT_UPSTREAM_ERROR");
                }
                List<String> selected=new ArrayList<>(); String answer=text;
                try {
                    var structured=parseReply(text);
                    if (!structured.path("answer").isTextual()) throw new IllegalArgumentException();
                    if (structured.has("productIds") && !structured.path("productIds").isArray()) throw new IllegalArgumentException();
                    answer=structured.path("answer").asText();
                    if (answer.isBlank() || answer.length()>6000 || structured.path("productIds").size()>3) throw new IllegalArgumentException();
                    for (var id:structured.path("productIds")) {
                        if (!id.isTextual() || !candidates.containsKey(id.asText())) throw new IllegalArgumentException();
                        if (!selected.contains(id.asText())) selected.add(id.asText());
                    }
                } catch (Exception e) {
                    log.warn("Assistant reply format fallback: turnId={}, model={}, exception={}",
                            turn.getId(), client.model(), e.getClass().getSimpleName());
                    selected.clear(); selected.addAll(candidates.keySet().stream().limit(3).toList());
                    // Never display malformed JSON or imply a personalized selection in fallback.
                    if (candidates.isEmpty()) {
                        String trimmed=text.strip();
                        if (trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("```"))
                            throw error("ASSISTANT_INVALID_RESPONSE");
                        answer=text;
                    } else answer="以下是本轮查询到的相关商品，请核对适用条件。";
                }
                List<ProductCard> cards=new ArrayList<>();
                for (String id:selected) {
                    // Preserve the budget bound selected by the tool if prices changed during the turn.
                    var fresh=products.get(Long.valueOf(id),user,candidates.get(id).priceCents());
                    if (fresh!=null) cards.add(fresh);
                }
                alternativeIds.removeAll(directMatchIds);
                long alternativeCount = cards.stream().filter(card -> alternativeIds.contains(card.id())).count();
                if (alternativeCount > 0) {
                    answer = alternativeCount == cards.size()
                            ? "没有找到你要的商品。下面是同类推荐商品，供你参考。"
                            : "部分指定商品没有找到。下面的商品卡片包含同类推荐，供你参考。";
                }
                if (System.nanoTime()>=deadline) throw error("ASSISTANT_TIMEOUT");
                transactions.finish(turn,answer,json.writeValueAsString(cards),prompt,completion);
                return view(turns.selectById(turn.getId()));
            }
            throw error("ASSISTANT_TOOL_LIMIT");
        } catch (Exception e) {
            String category=e instanceof BusinessException ? ((BusinessException)e).getMessage() : "ASSISTANT_UPSTREAM_ERROR";
            log.warn("Assistant turn failed: turnId={}, sessionId={}, model={}, category={}, exception={}",
                    turn.getId(), session, client.model(), category, e.getClass().getSimpleName());
            transactions.fail(turn,category); throw error(category);
        }
    }
    // Tolerate Markdown fencing and a single JSON-string wrapper without exposing raw JSON.
    JsonNode parseReply(String text) throws java.io.IOException {
        String normalized=text.strip();
        if (normalized.startsWith("```")) {
            int newline=normalized.indexOf('\n');
            if (newline>=0 && normalized.endsWith("```"))
                normalized=normalized.substring(newline+1,normalized.length()-3).strip();
        }
        JsonNode node=json.readTree(normalized);
        if (node!=null && node.isTextual()) node=json.readTree(node.asText());
        return node;
    }
    private com.fasterxml.jackson.databind.node.ObjectNode message(String role,String content) {
        return json.createObjectNode().put("role",role).put("content",content);
    }
    public TurnView view(AssistantTurn turn) {
        List<ProductCard> cards=List.of();
        if (turn.getProductsJson()!=null) try {
            cards=json.readValue(turn.getProductsJson(),json.getTypeFactory().constructCollectionType(List.class,ProductCard.class));
        } catch (Exception e) { throw error("ASSISTANT_HISTORY_ERROR"); }
        String state=turn.getStatus();
        if ("PENDING".equals(state) && turn.getLeaseUntil()!=null && turn.getLeaseUntil().isBefore(java.time.LocalDateTime.now())) state="FAILED";
        return new TurnView(turn.getSessionId().toString(),turn.getId().toString(),turn.getRequestId(),state,
                turn.getQuestion(),turn.getAnswer(),cards,turn.getCreatedAt());
    }
}
