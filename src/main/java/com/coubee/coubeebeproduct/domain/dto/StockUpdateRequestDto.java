package com.coubee.coubeebeproduct.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateRequestDto {
    private Long storeId;
    private List<StockUpdateDto> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockUpdateDto {
        private Long productId;
        private Integer quantityChange;
    }

}
