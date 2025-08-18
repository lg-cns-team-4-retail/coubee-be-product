package com.coubee.coubeebeproduct.api.open;

import com.coubee.coubeebeproduct.common.dto.ApiResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductSearchResponse;
import com.coubee.coubeebeproduct.domain.elasticsearch.ProductDocument;
import com.coubee.coubeebeproduct.remote.store.RemoteStoreService;
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
    private final RemoteStoreService remoteStoreService;

    @GetMapping("/search")
    public List<ProductDocument> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return productSearchService.searchByName(keyword, page, size);
    }
    @GetMapping("/search/es")
    public ApiResponseDto<List<ProductResponseDto>> searchProducts(@RequestParam double latitude, @RequestParam double longitude,@RequestParam(required = false,defaultValue = "") String keyword) {
        log.info("keyword :{}", keyword);
        List<Long> storeIds = remoteStoreService.getNearStoreIds(latitude, longitude).getData();
        log.info("storeIds :{}", storeIds);
        List<Long> productIds = productSearchService.nearStoreSearchProducts(keyword,storeIds);
        List<ProductResponseDto> list = productService.getProductsByProductIds(productIds);
        return ApiResponseDto.readOk(list);
    }
    @GetMapping("/detail/{productId}")
    public ApiResponseDto<ProductResponseDto> getProductById(@PathVariable Long productId) {
        ProductResponseDto dto = productService.getProductById(productId);
        return ApiResponseDto.readOk(dto);
    }
}
