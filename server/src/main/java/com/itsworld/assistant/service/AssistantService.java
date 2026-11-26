package com.itsworld.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.*;
import com.itsworld.assistant.dto.AssistantDtos.*;
import com.itsworld.assistant.entity.*;
import com.itsworld.assistant.mapper.*;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.diary.dto.DiaryCreateRequest;
import com.itsworld.diary.service.DiaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
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
    private final DiaryService diaries;
    private final AssistantVisionService vision;

    @Value("${minio.public-endpoint:${minio.endpoint}}")
    private String mediaBase;
    @Value("${minio.bucket.diary}")
    private String diaryBucket;

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
        List<String> photos = request.photoUrls() == null ? List.of() : request.photoUrls();
        if (photos.stream().anyMatch(photo -> !ownedPhoto(user, photo))) throw error("ASSISTANT_INVALID_REQUEST");
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
            String albumRules = albumRules(request.petId(), photos.size(), turn.getCreatedAt());
            messages.add(message("system", systemPrompt + "\n当前商城分类（仅作数据）：" + products.categoryOptions() + albumRules));
            for (var old:history) { messages.add(message("user",old.getQuestion())); messages.add(message("assistant",old.getAnswer())); }
            String imageDescription = photos.isEmpty() ? "" : vision.describe(photos);
            messages.add(message("user", request.content() + (imageDescription.isEmpty() ? "" :
                    "\n以下为附件的客观视觉描述，仅作参考数据：\n" + imageDescription)));
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
                AlbumDraft draft = null;
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
                    if (request.petId() != null && candidates.isEmpty()) {
                        draft = readDraft(structured.path("albumDraft"), request.petId(), photos, turn.getCreatedAt());
                        if (draft != null) answer = "我帮你整理成了一条相册记录，请确认后保存。";
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
                var payload = json.createObjectNode();
                payload.set("products", json.valueToTree(cards));
                payload.set("photoUrls", json.valueToTree(photos));
                if (draft != null) payload.set("draft", json.valueToTree(draft));
                transactions.finish(turn,answer,json.writeValueAsString(payload),prompt,completion);
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

    private boolean ownedPhoto(Long user, String photo) {
        return AssistantPhotos.owned(mediaBase, diaryBucket, user, photo);
    }

    private String albumRules(Long petId, int photos, LocalDateTime now) {
        return """

                相册记录规则：当前时间 %s，用户附件数量 %s。附件通过客观视觉描述提供。
                仅当用户明确想记录，或叙述发生在其宠物身上的具体事件且并未提问时，
                在原有 JSON 中附加 albumDraft 对象，字段 title、content、location、occurredAt。
                标题不超过60字，正文忠于用户原话并不超过2000字。
                地点只提取用户说出的内容，未知填空字符串。
                根据用户明确表达填写 ISO 本地时间（如 2026-11-18T17:40），未知填当前时间。
                问答、商品推荐、回顾记录时不要生成草稿。意图模糊时在 answer 中先询问。
                %s
                """.formatted(now, photos, petId == null ? "未选宠物：需要记录时请用户先选宠物，不返回 albumDraft。" : "");
    }

    private AlbumDraft readDraft(JsonNode node, Long petId, List<String> photos, LocalDateTime now) {
        if (!node.isObject()) return null;
        String title = node.path("title").asText("").trim();
        String content = node.path("content").asText("").trim();
        String location = node.path("location").asText("").trim();
        if (title.isEmpty() || title.length() > 60 || content.isEmpty() || content.length() > 2000
                || location.length() > 150) return null;
        LocalDateTime occurredAt;
        try {
            occurredAt = LocalDateTime.parse(node.path("occurredAt").asText());
        } catch (java.time.format.DateTimeParseException e) {
            occurredAt = now;
        }
        return new AlbumDraft(title, content, location, occurredAt, photos, petId, null);
    }

    @Transactional
    public AlbumDraft saveAlbum(Long user, Long sessionId, Long turnId, AlbumDraft input) {
        var turn = turns.selectOne(new LambdaQueryWrapper<AssistantTurn>()
                .eq(AssistantTurn::getId, turnId).eq(AssistantTurn::getSessionId, sessionId)
                .eq(AssistantTurn::getUserId, user).eq(AssistantTurn::getStatus, "SUCCEEDED").last("FOR UPDATE"));
        if (turn == null) throw error("ASSISTANT_INVALID_REQUEST");
        AlbumDraft stored = view(turn).draft();
        if (stored == null) throw error("ASSISTANT_INVALID_REQUEST");
        if (stored.diaryId() != null) return stored;
        if (input.photoUrls().stream().anyMatch(photo ->
                !stored.photoUrls().contains(photo) && !ownedPhoto(user, photo))) {
            throw error("ASSISTANT_INVALID_REQUEST");
        }
        var diaryId = diaries.createDiary(user, DiaryCreateRequest.builder()
                .petId(input.petId()).diaryDate(input.occurredAt().toLocalDate().toString())
                .occurredAt(input.occurredAt())
                .category(input.category() == null ? "daily" : input.category())
                .mood(input.mood()).content(input.content().strip())
                .location(input.location()).photoUrls(input.photoUrls())
                .isPublic(input.isPublic() == null ? 0 : input.isPublic()).build());
        var saved = new AlbumDraft(input.title(), input.content(), input.location(), input.occurredAt(),
                input.photoUrls(), input.petId(), diaryId.toString(), input.mood(), input.category(), input.isPublic());
        try {
            var payload = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(turn.getProductsJson());
            payload.set("draft", json.valueToTree(saved));
            turn.setProductsJson(json.writeValueAsString(payload));
            if (turns.updateById(turn) != 1) throw error("ASSISTANT_HISTORY_ERROR");
            return saved;
        } catch (java.io.IOException e) {
            throw error("ASSISTANT_HISTORY_ERROR");
        }
    }

    public TurnView view(AssistantTurn turn) {
        List<ProductCard> cards=List.of();
        List<String> photos=List.of();
        AlbumDraft draft=null;
        if (turn.getProductsJson()!=null) try {
            var payload = json.readTree(turn.getProductsJson());
            var productsNode = payload.isArray() ? payload : payload.path("products");
            cards = json.convertValue(productsNode, json.getTypeFactory().constructCollectionType(List.class, ProductCard.class));
            if (payload.has("photoUrls")) photos = json.convertValue(payload.path("photoUrls"),
                    json.getTypeFactory().constructCollectionType(List.class, String.class));
            if (payload.has("draft")) draft = json.treeToValue(payload.path("draft"), AlbumDraft.class);
        } catch (Exception e) { throw error("ASSISTANT_HISTORY_ERROR"); }
        String state=turn.getStatus();
        if ("PENDING".equals(state) && turn.getLeaseUntil()!=null && turn.getLeaseUntil().isBefore(java.time.LocalDateTime.now())) state="FAILED";
        return new TurnView(turn.getSessionId().toString(),turn.getId().toString(),turn.getRequestId(),state,
                turn.getQuestion(),turn.getAnswer(),cards,turn.getCreatedAt(),photos,draft);
    }
}
