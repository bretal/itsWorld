package com.itsworld.assistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.common.result.Result;
import com.itsworld.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/assistant/speech")
public class AssistantSpeechController {
    private final ObjectMapper json;
    private final com.itsworld.security.service.CurrentUserService users;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<Long, Long> lastCall = new ConcurrentHashMap<>();
    @Value("${SPEECH_TENCENT_SECRET_ID:}") private String secretId;
    @Value("${SPEECH_TENCENT_SECRET_KEY:}") private String secretKey;
    @Value("${SPEECH_TENCENT_APP_ID:}") private String appId;

    public AssistantSpeechController(ObjectMapper json,
            com.itsworld.security.service.CurrentUserService users) {
        this.json = json;
        this.users = users;
    }

    @GetMapping("/status")
    public Result<Boolean> status() { return Result.success(!secretId.isBlank() && !secretKey.isBlank() && !appId.isBlank()); }

    @PostMapping("/connection")
    public Result<String> connection(@RequestParam(defaultValue = "8") int voiceFormat) {
        if (voiceFormat != 1 && voiceFormat != 8) throw failure("不支持的录音格式");
        if (!status().getData()) throw failure("实时语音尚未配置");
        long now = Instant.now().getEpochSecond();
        long accepted = lastCall.compute(users.getUserId(), (id, previous) ->
                previous == null || now * 1000 - previous >= 10_000 ? now * 1000 : previous);
        if (accepted != now * 1000) throw failure("语音请求较频繁，请稍后重试");
        try {
            var params = new TreeMap<String, String>();
            params.put("engine_model_type", "16k_zh");
            params.put("expired", String.valueOf(now + 120));
            params.put("needvad", "1");
            params.put("nonce", String.valueOf(new java.security.SecureRandom().nextInt(Integer.MAX_VALUE)));
            params.put("secretid", secretId);
            params.put("timestamp", String.valueOf(now));
            params.put("voice_format", String.valueOf(voiceFormat));
            params.put("voice_id", UUID.randomUUID().toString());
            String query = params.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                    .collect(java.util.stream.Collectors.joining("&"));
            String path = "asr.cloud.tencent.com/asr/v2/" + appId + "?" + query;
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            String signature = Base64.getEncoder().encodeToString(mac.doFinal(path.getBytes(StandardCharsets.UTF_8)));
            return Result.success("wss://" + path + "&signature="
                    + java.net.URLEncoder.encode(signature, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw failure("实时语音连接创建失败");
        }
    }

    @PostMapping
    public Result<String> transcribe(@RequestParam MultipartFile file) {
        if (secretId.isBlank() || secretKey.isBlank()) throw failure("语音转写尚未配置");
        if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024) throw failure("录音文件为空或超过大小限制");
        long now = System.currentTimeMillis();
        long accepted = lastCall.compute(users.getUserId(), (id, previous) ->
                previous == null || now - previous >= 10_000 ? now : previous);
        if (accepted != now) throw failure("语音请求较频繁，请稍后重试");
        try {
            byte[] audio = file.getBytes();
            String body = json.writeValueAsString(Map.of("EngSerViceType", "16k_zh",
                    "SourceType", 1, "VoiceFormat", "mp3", "Data", Base64.getEncoder().encodeToString(audio),
                    "DataLen", audio.length));
            long timestamp = Instant.now().getEpochSecond();
            String date = Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC).toLocalDate().toString();
            String headers = "content-type:application/json; charset=utf-8\nhost:asr.tencentcloudapi.com\n";
            String canonical = "POST\n/\n\n" + headers + "\ncontent-type;host\n" + sha(body);
            String scope = date + "/asr/tc3_request";
            String signText = "TC3-HMAC-SHA256\n" + timestamp + "\n" + scope + "\n" + sha(canonical);
            byte[] dateKey = hmac(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
            byte[] signing = hmac(hmac(dateKey, "asr"), "tc3_request");
            String signature = HexFormat.of().formatHex(hmac(signing, signText));
            String auth = "TC3-HMAC-SHA256 Credential=" + secretId + "/" + scope
                    + ", SignedHeaders=content-type;host, Signature=" + signature;
            var request = HttpRequest.newBuilder(URI.create("https://asr.tencentcloudapi.com"))
                    .timeout(Duration.ofSeconds(25)).header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", auth).header("X-TC-Action", "SentenceRecognition")
                    .header("X-TC-Version", "2019-06-14").header("X-TC-Timestamp", String.valueOf(timestamp))
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            var result = json.readTree(response.body()).path("Response");
            if (response.statusCode() != 200 || result.has("Error")) throw failure("语音转写失败，请稍后重试");
            String text = result.path("Result").asText("").strip();
            if (text.isEmpty()) throw failure("没有识别到语音，请重录");
            return Result.success(text);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure("语音转写中断，请重试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw failure("语音转写失败，请稍后重试");
        }
    }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static byte[] hmac(byte[] key, String text) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
    }
    private static BusinessException failure(String message) { return new BusinessException(1500, message); }
}
