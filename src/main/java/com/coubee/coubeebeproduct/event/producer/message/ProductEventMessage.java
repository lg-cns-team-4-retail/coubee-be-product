package com.coubee.coubeebeproduct.event.producer.message;

import com.coubee.coubeebeproduct.domain.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductEventMessage {
    public static final String Topic = "product-event";

    private String action;
    private TargetProduct targetProduct;

    @Getter
    @Setter
    public static class TargetProduct {
        private Long productId;
        private String productName;
        private String description;
        private Long storeId;
    }

    public static ProductEventMessage fromEntity(String action, Product product) {
        ProductEventMessage event = new ProductEventMessage();

        event.action = action;

        if (product != null) {
            event.targetProduct = new TargetProduct();
            event.targetProduct.productId = product.getProductId();
            event.targetProduct.productName = product.getProductName();
            event.targetProduct.description = product.getDescription();
            event.targetProduct.storeId = product.getStoreId();
        }
        return event;
    }
}
