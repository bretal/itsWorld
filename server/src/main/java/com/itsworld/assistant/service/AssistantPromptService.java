package com.itsworld.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.assistant.mapper.AssistantPromptMapper;
import com.itsworld.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssistantPromptService {
    private static final List<String> DEFAULT_PROMPTS = List.of(
            "今天毛孩子状态怎么样？", "看看有没有毛孩子爱吃的？",
            "今天给毛孩子梳毛了吗？", "最近给毛孩子拍新照片了吗？",
            "想给毛孩子安排一次护理吗？");

    private final AssistantPromptMapper mapper;
    private final ObjectMapper json;

    public List<String> list() {
        String stored = mapper.findPrompts();
        if (stored == null) {
            return DEFAULT_PROMPTS;
        }
        try {
            return json.readValue(stored, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid stored assistant prompts", e);
        }
    }

    public List<String> save(List<String> prompts) {
        if (prompts == null || prompts.isEmpty() || prompts.size() > 20
                || prompts.stream().anyMatch(text -> text == null || text.isBlank() || text.strip().length() > 40)) {
            throw new BusinessException("请输入 1 到 20 句文案，每句最多 40 字");
        }
        List<String> normalized = prompts.stream().map(String::strip).distinct().toList();
        try {
            mapper.savePrompts(json.writeValueAsString(normalized));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to encode assistant prompts", e);
        }
        return normalized;
    }
}
