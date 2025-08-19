package com.coubee.coubeebeproduct.event.consumer.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDecreaseEvent {
    private String eventId; // 이벤트 고유 ID (멱등성 처리에 사용)
    private String orderId;
    private Long userId;
    private LocalDateTime timestamp;
    private List<StockItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockItem {
        private Long productId;
        private Integer quantity;
    }
}