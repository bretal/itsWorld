package com.itsworld.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import static com.itsworld.assistant.service.AssistantErrors.error;

@lombok.extern.slf4j.Slf4j
@Service
public class DeepSeekClient {
    private final ObjectMapper json;
    private final HttpClient http;
    private final boolean enabled;
    private final String key;
    private final String baseUrl;
    private final String model;

    public DeepSeekClient(ObjectMapper json,
            @Value("${ai.deepseek.enabled:false}") boolean enabled,
            @Value("${ai.deepseek.api-key:}") String key,
            @Value("${ai.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${ai.deepseek.model:deepseek-flash}") String model) {
        this.json=json; this.enabled=enabled; this.key=key; this.baseUrl=baseUrl;
        this.model=model; this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }
    public String model() { return model; }
    public void requireConfigured() {
        if (!enabled || key.isBlank()) throw error("ASSISTANT_NOT_CONFIGURED");
    }
    public JsonNode complete(List<JsonNode> messages, JsonNode tools, long deadline) {
        requireConfigured();
        long remaining=deadline-System.nanoTime();
        if (remaining<=0) throw error("ASSISTANT_TIMEOUT");
        try {
            var body=json.createObjectNode().put("model",model).put("max_tokens",900).put("stream",false);
            body.set("messages",json.valueToTree(messages));
            body.set("thinking",json.createObjectNode().put("type","disabled"));
            if (tools!=null) body.set("tools",tools);
            var request=HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$","")+"/chat/completions"))
                    .timeout(Duration.ofNanos(Math.min(remaining,Duration.ofSeconds(20).toNanos())))
                    .header("Authorization","Bearer "+key).header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofString());
            if (response.statusCode()!=200) {
                String category=switch(response.statusCode()) {
                    case 401 -> "AUTH"; case 402 -> "BALANCE"; case 429 -> "RATE_LIMIT";
                    default -> response.statusCode()>=500 ? "SERVER" : "REJECTED";
                };
                log.warn("DeepSeek request rejected: model={}, status={}, category={}", model, response.statusCode(), category);
                throw error("ASSISTANT_UPSTREAM_"+category);
            }
            JsonNode result=json.readTree(response.body());
            if (result == null || !result.path("choices").path(0).path("message").isObject()) {
                log.warn("DeepSeek response missing message: model={}", model);
                throw error("ASSISTANT_UPSTREAM_ERROR");
            }
            return result;
        } catch (HttpTimeoutException e) { throw error("ASSISTANT_TIMEOUT");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw error("ASSISTANT_TIMEOUT");
        } catch (com.itsworld.common.exception.BusinessException e) { throw e;
        } catch (Exception e) {
            log.warn("DeepSeek request or response decoding failed: model={}, exception={}", model, e.getClass().getSimpleName());
            throw error("ASSISTANT_UPSTREAM_ERROR");
        }
    }
}
