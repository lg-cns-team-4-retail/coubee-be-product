package com.coubee.coubeebeproduct.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRegisterDto {
    private String productName;
    private String description;
    private String productImg;
    private int originPrice;
    private int salePrice;
    private int stock;
    private Long storeId;
}
