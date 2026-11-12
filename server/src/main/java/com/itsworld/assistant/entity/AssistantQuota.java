package com.itsworld.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("assistant_user_quota")
public class AssistantQuota {
    @TableId(type = IdType.INPUT)
    private Long userId;
    private LocalDateTime hourStart;
    private Integer attempts;
    private String activeRequestId;
    private String leaseToken;
    private LocalDateTime leaseUntil;
    private LocalDateTime updatedAt;
}
