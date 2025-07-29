package com.coubee.coubeebeproduct.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateDto {
    private Long productId;
    private String productName;
    private String description;
    private String productImg;
    private int originPrice;
    private int salePrice;
    private int stock;
}
