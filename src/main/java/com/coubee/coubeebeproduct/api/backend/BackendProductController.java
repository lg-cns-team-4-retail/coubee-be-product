package com.coubee.coubeebeproduct.api.backend;


import com.coubee.coubeebeproduct.common.dto.ApiResponseDto;
import com.coubee.coubeebeproduct.domain.dto.StockUpdateRequestDto;
import com.coubee.coubeebeproduct.service.BackendProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/backend/product")
public class BackendProductController {

    private final BackendProductService backendProductService;

    @PostMapping("/stock/update")
    public ApiResponseDto<String> stockUpdate(@RequestBody StockUpdateRequestDto dto){
        return backendProductService.updateStocks(dto);
    }
}
