package com.coubee.coubeebeproduct.api.open;

import com.coubee.coubeebeproduct.common.dto.ApiResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductSearchResponse;
import com.coubee.coubeebeproduct.domain.elasticsearch.ProductDocument;
import com.coubee.coubeebeproduct.service.ProductSearchService;
import com.coubee.coubeebeproduct.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/product", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProductController {

    private final ProductSearchService productSearchService;
    private final ProductService productService;

    @GetMapping("/search")
    public List<ProductDocument> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return productSearchService.searchByName(keyword, page, size);
    }
    @GetMapping("/search/es")
    public ApiResponseDto<List<ProductSearchResponse>> searchProducts(@RequestParam String keyword) {
        log.info("keyword :{}", keyword);
        List<ProductSearchResponse> list = productSearchService.hybridSearch(keyword);
        return ApiResponseDto.readOk(list);
    }
    @GetMapping("/detail/{productId}")
    public ApiResponseDto<ProductResponseDto> getProductById(@PathVariable Long productId) {
        ProductResponseDto dto = productService.getProductById(productId);
        return ApiResponseDto.readOk(dto);
    }
}
