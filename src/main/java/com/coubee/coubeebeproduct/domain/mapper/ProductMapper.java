package com.coubee.coubeebeproduct.domain.mapper;

import com.coubee.coubeebeproduct.domain.Product;
import com.coubee.coubeebeproduct.domain.ProductStatus;
import com.coubee.coubeebeproduct.domain.dto.ProductRegisterDto;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;

public class ProductMapper {


    public static Product toEntity(ProductRegisterDto dto){
        return Product.builder()
                .productName(dto.getProductName())
                .productImg(dto.getProductImg())
                .originPrice(dto.getOriginPrice())
                .salePrice(dto.getSalePrice())
                .description(dto.getDescription())
                .stock(dto.getStock())
                .storeId(dto.getStoreId())
                .status(ProductStatus.ACTIVE)
                .build();
    }

    public static ProductResponseDto fromEntity(Product product) {
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
