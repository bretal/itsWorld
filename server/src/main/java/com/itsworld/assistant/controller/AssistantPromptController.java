package com.itsworld.assistant.controller;

import com.itsworld.assistant.service.AssistantPromptService;
import com.itsworld.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AssistantPromptController {
    private final AssistantPromptService prompts;

    @GetMapping({"/api/assistant/prompts", "/api/admin/assistant/prompts"})
    public Result<List<String>> list() {
        return Result.success(prompts.list());
    }

    @PutMapping("/api/admin/assistant/prompts")
    public Result<List<String>> save(@RequestBody List<String> values) {
        return Result.success(prompts.save(values));
    }
}
