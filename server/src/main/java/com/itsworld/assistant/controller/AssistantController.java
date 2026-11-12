package com.itsworld.assistant.controller;
import com.itsworld.assistant.dto.AssistantDtos.*;
import com.itsworld.assistant.service.AssistantService;
import com.itsworld.common.result.Result;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/assistant/sessions")
@RequiredArgsConstructor
public class AssistantController {
    private final AssistantService assistant;
    private final CurrentUserService currentUser;
    @PostMapping public Result<SessionView> create() { return Result.success(assistant.create(currentUser.getUserId())); }
    @GetMapping public Result<List<SessionView>> sessions() { return Result.success(assistant.sessions(currentUser.getUserId())); }
    @GetMapping("/{id}/messages") public Result<History> history(@PathVariable Long id,@RequestParam(required=false) Long beforeId) {
        return Result.success(assistant.history(currentUser.getUserId(),id,beforeId));
    }
    @PostMapping("/{id}/messages") public Result<TurnView> send(@PathVariable Long id,@Valid @RequestBody SendRequest request) {
        return Result.success(assistant.send(currentUser.getUserId(),id,request));
    }
}
