package com.coubee.coubeebeproduct.api.backend;


import com.coubee.coubeebeproduct.common.dto.ApiResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.StockUpdateRequestDto;
import com.coubee.coubeebeproduct.service.BackendProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/backend/product")
@Slf4j
public class BackendProductController {

    private final BackendProductService backendProductService;

    @PostMapping("/stock/update")
    public ApiResponseDto<String> stockUpdate(@RequestBody StockUpdateRequestDto dto){
        log.info("stockUpdate dto storeId:{}", dto.getStoreId());
        return backendProductService.updateStocks(dto);
    }
    @GetMapping("/bulk")
    public ApiResponseDto<Map<Long, ProductResponseDto>> getProductsByIds(@RequestParam(name = "productIds") List<Long> productIds){
        Map<Long, ProductResponseDto> productMap = backendProductService.getProductsByIds(productIds);
        return ApiResponseDto.readOk(productMap);
    }
}
