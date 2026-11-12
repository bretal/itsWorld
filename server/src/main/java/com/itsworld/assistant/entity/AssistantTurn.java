package com.itsworld.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("assistant_turn")
public class AssistantTurn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private String requestId;
    private String contentHash;
    private String question;
    private String answer;
    private String productsJson;
    private String status;
    private Integer attemptCount;
    private String leaseToken;
    private LocalDateTime leaseUntil;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private String errorCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
