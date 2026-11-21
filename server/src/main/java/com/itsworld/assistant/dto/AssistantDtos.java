package com.itsworld.assistant.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class AssistantDtos {
    private AssistantDtos() {}
    public record SendRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,64}") String requestId,
                              @NotBlank @Size(max=2000) String content, @Positive Long petId,
                              @Size(max=3) List<@NotBlank @Size(max=500) String> photoUrls) {
        public SendRequest(String requestId, String content) { this(requestId, content, null, null); }
        public SendRequest(String requestId, String content, Long petId) { this(requestId, content, petId, null); }
    }
    public record AlbumDraft(@NotBlank @Size(max=60) String title, @NotBlank @Size(max=2000) String content,
                             @Size(max=150) String location, @NotNull LocalDateTime occurredAt,
                             @NotNull @Size(max=9) List<@NotBlank String> photoUrls,
                             @NotNull @Positive Long petId, String diaryId,
                             @Pattern(regexp="happy|calm|tired|sad") String mood,
                             @Pattern(regexp="daily|food|health") String category,
                             @Min(0) @Max(1) Integer isPublic) {
        public AlbumDraft(String title, String content, String location, LocalDateTime occurredAt,
                          List<String> photoUrls, Long petId, String diaryId) {
            this(title, content, location, occurredAt, photoUrls, petId, diaryId, null, "daily", 0);
        }
    }
    public record SessionView(String id, String title, LocalDateTime updatedAt) {}
    public record ProductCard(String id, String name, String subtitle, String coverUrl,
                              String skuId, String specText, long priceCents,
                              Long memberPriceCents, int availableQty, String detailPath) {}
    public record TurnView(String sessionId, String turnId, String requestId, String status,
                           String question, String content, List<ProductCard> products,
                           LocalDateTime createdAt, List<String> photoUrls, AlbumDraft draft) {}
    public record History(List<TurnView> records, String nextBeforeId, boolean hasMore) {}
}
