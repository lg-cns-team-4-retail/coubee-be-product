package com.coubee.coubeebeproduct.service;

import com.coubee.coubeebeproduct.common.dto.ApiResponseDto;
import com.coubee.coubeebeproduct.common.exception.BadParameter;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.StockUpdateRequestDto;
import com.coubee.coubeebeproduct.domain.mapper.ProductMapper;
import com.coubee.coubeebeproduct.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackendProductService {
    private final ProductRepository productRepository;

    @Transactional
    public ApiResponseDto<String> updateStocks(StockUpdateRequestDto dto) {
        try {
            dto.getItems().forEach(stockUpdateDto -> {
                productRepository.findById(stockUpdateDto.getProductId())
                        .ifPresentOrElse(product -> {
                            int updated = productRepository.updateStock(
                                    stockUpdateDto.getProductId(),
                                    stockUpdateDto.getQuantityChange()
                            );
                            if (updated == 0) {
                                throw new BadParameter("상품(" + stockUpdateDto.getProductId() + ")의 재고가 부족하거나 존재하지 않습니다");
                            }
                        }, () -> {
                            throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + stockUpdateDto.getProductId());
                        });
            });
            return ApiResponseDto.defaultOk();
        } catch (BadParameter e) {
            return ApiResponseDto.createError("VALIDATION_ERROR", e.getMessage());
        } catch (Exception e) {
            return ApiResponseDto.createError("ERROR", "재고 수정 실패: " + e.getMessage());
        }
    }
    @Transactional(readOnly = true)
    public Map<Long, ProductResponseDto> getProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productRepository.findAllById(productIds).stream()
                .map(ProductMapper::fromEntity)
                .collect(Collectors.toMap(ProductResponseDto::getProductId, Function.identity()));
    }
}
