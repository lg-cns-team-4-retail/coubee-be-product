package com.coubee.coubeebeproduct.domain.dto;

import com.coubee.coubeebeproduct.domain.Product;
import lombok.Data;

@Data
public class ProductResponseDto {

    private Long productId;

    private String productName;

    private String description;

    private String productImg;

    private int originPrice;

    private int salePrice;

    private int stock;

    private Long storeId;

    public static ProductResponseDto from(Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setDescription(product.getDescription());
        dto.setProductImg(product.getProductImg());
        dto.setOriginPrice(product.getOriginPrice());
        dto.setSalePrice(product.getSalePrice());
        dto.setStock(product.getStock());
        dto.setStoreId(product.getStoreId());
        return dto;
    }
}
