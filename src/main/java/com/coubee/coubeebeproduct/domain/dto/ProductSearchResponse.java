package com.coubee.coubeebeproduct.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSearchResponse {
    private String productName;
    private Integer productId;
    private Double score;
}