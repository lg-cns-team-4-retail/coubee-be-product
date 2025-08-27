package com.coubee.coubeebeproduct.event.consumer.message;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDecreaseEvent {
    public static final String Topic = "stock_decrease";
    private String eventId; // 이벤트 고유 ID (멱등성 처리에 사용)
    private String orderId;
    private Long userId;
    private LocalDateTime timestamp;
    private List<StockItem> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockItem {
        private Long productId;
        private Integer quantity;
    }
}