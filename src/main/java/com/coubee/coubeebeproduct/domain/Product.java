package com.coubee.coubeebeproduct.domain;

import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductUpdateDto;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@Data
@NoArgsConstructor
public class Product extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(length = 100, nullable = false)
    private String productName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String productImg;

    @Column(nullable = false)
    private int originPrice;

    @Column(nullable = false)
    private int salePrice;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private Long storeId;

    @Column
    @Enumerated(EnumType.STRING)
    @Setter
    public ProductStatus status;

    @Builder
    public Product(String productName,String description,String productImg,int originPrice,int salePrice,int stock,Long storeId,ProductStatus status) {
        this.productName = productName;
        this.description = description;
        this.productImg = productImg;
        this.originPrice = originPrice;
        this.salePrice = salePrice;
        this.stock = stock;
        this.storeId = storeId;
        this.status = status;
    }

    public void updateProduct(ProductUpdateDto updateDto) {
        this.productName = updateDto.getProductName();
        this.description = updateDto.getDescription();
        if(updateDto.getProductImg() != null&& !updateDto.getProductImg().isEmpty()) {
            this.productImg = updateDto.getProductImg();
        }
        this.originPrice = updateDto.getOriginPrice();
        this.salePrice = updateDto.getSalePrice();
        this.stock = updateDto.getStock();
    }
}