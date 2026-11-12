package com.itsworld.assistant.controller;

import com.itsworld.assistant.service.AssistantInstructionService;
import com.itsworld.assistant.service.AssistantInstructionService.*;
import com.itsworld.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/admin/assistant/instructions")
@RequiredArgsConstructor
public class AssistantInstructionController {
    private final AssistantInstructionService service;
    public record PreviewRequest(String question, boolean withSamplePet) {}

    @GetMapping public Result<Settings> get() { return Result.success(service.settings()); }
    @PutMapping("/draft") public Result<Settings> draft(@RequestBody Config config) { return Result.success(service.saveDraft(config)); }
    @PostMapping("/publish") public Result<Settings> publish() { return Result.success(service.publish()); }
    @PostMapping("/rollback") public Result<Settings> rollback() { return Result.success(service.rollback()); }
    @PostMapping("/preview") public Result<Preview> preview(@RequestBody PreviewRequest request) throws IOException {
        return Result.success(service.preview(request.question(), request.withSamplePet()));
    }
}
