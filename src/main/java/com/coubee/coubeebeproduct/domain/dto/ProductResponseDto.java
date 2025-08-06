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
}
