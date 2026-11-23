package com.itsworld.assistant.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itsworld.assistant.service.AssistantPageTaskService;
import com.itsworld.assistant.service.AssistantPageTaskService.*;
import com.itsworld.common.result.Result;
import com.itsworld.health.dto.WeightRecordCreateRequest;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant/tasks")
@RequiredArgsConstructor
public class AssistantPageTaskController {
    private final AssistantPageTaskService tasks;
    private final CurrentUserService currentUser;

    @PostMapping
    public Result<ObjectNode> create(@Valid @RequestBody CreateRequest request) {
        return Result.success(tasks.create(currentUser.getUserId(), request));
    }

    @GetMapping("/{id}")
    public Result<ObjectNode> get(@PathVariable String id) {
        return Result.success(tasks.get(currentUser.getUserId(), id));
    }

    @PostMapping("/{id}/results")
    public Result<ObjectNode> advance(@PathVariable String id, @Valid @RequestBody StepRequest request) {
        return Result.success(tasks.advance(currentUser.getUserId(), id, request));
    }

    @PostMapping("/{id}/cancel")
    public Result<ObjectNode> cancel(@PathVariable String id) {
        return Result.success(tasks.cancel(currentUser.getUserId(), id));
    }

    @PostMapping("/{id}/actions/{actionId}/weight")
    public Result<String> saveWeight(@PathVariable String id, @PathVariable String actionId,
                                     @Valid @RequestBody WeightRecordCreateRequest request) {
        return Result.success(tasks.saveWeight(currentUser.getUserId(), id, actionId, request));
    }
}
