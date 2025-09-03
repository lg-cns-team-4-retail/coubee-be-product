package com.coubee.coubeebeproduct.event.producer.message;

import com.coubee.coubeebeproduct.domain.Product;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductEventMessage {
    public static final String Topic = "product-event";

    private String eventType;
    private TargetProduct targetProduct;

    @Getter
    @Setter
    public static class TargetProduct {
        private Long productId;
        private String productName;
        private String description;
        private Long storeId;
        private LocalDateTime updatedAt;
    }

    public static ProductEventMessage fromEntity(String action, Product product) {
        ProductEventMessage event = new ProductEventMessage();
        event.eventType = action;
        if (product != null) {
            event.targetProduct = new TargetProduct();
            event.targetProduct.productId = product.getProductId();
            event.targetProduct.productName = product.getProductName();
            event.targetProduct.description = product.getDescription();
            event.targetProduct.storeId = product.getStoreId();
            event.targetProduct.updatedAt = product.getUpdatedAt();
        }
        return event;
    }
}
