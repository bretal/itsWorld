package com.itsworld.assistant.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class AssistantDtos {
    private AssistantDtos() {}
    public record SendRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,64}") String requestId,
                              @NotBlank @Size(max=2000) String content, @Positive Long petId) {
        public SendRequest(String requestId, String content) { this(requestId, content, null); }
    }
    public record SessionView(String id, String title, LocalDateTime updatedAt) {}
    public record ProductCard(String id, String name, String subtitle, String coverUrl,
                              String skuId, String specText, long priceCents,
                              Long memberPriceCents, int availableQty, String detailPath) {}
    public record TurnView(String sessionId, String turnId, String requestId, String status,
                           String question, String content, List<ProductCard> products,
                           LocalDateTime createdAt) {}
    public record History(List<TurnView> records, String nextBeforeId, boolean hasMore) {}
}
