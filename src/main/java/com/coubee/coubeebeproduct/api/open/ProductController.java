package com.coubee.coubeebeproduct.api.open;

import com.coubee.coubeebeproduct.common.dto.ApiResponseDto;
import com.coubee.coubeebeproduct.common.web.context.GatewayRequestHeaderUtils;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductSearchResponse;
import com.coubee.coubeebeproduct.domain.elasticsearch.ProductDocument;
import com.coubee.coubeebeproduct.remote.store.RemoteStoreService;
import com.coubee.coubeebeproduct.service.ProductSearchService;
import com.coubee.coubeebeproduct.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
//    @GetMapping("/search/es")
//    public ApiResponseDto<List<ProductResponseDto>> searchProducts(@RequestParam double latitude, @RequestParam double longitude,@RequestParam(required = false,defaultValue = "") String keyword) {
//        log.info("keyword :{}", keyword);
//        List<Long> storeIds = remoteStoreService.getNearStoreIds(latitude, longitude).getData();
//        log.info("storeIds :{}", storeIds);
//        List<Long> productIds = productSearchService.nearStoreSearchProducts(keyword,storeIds);
//        List<ProductResponseDto> list = productService.getProductsByProductIds(productIds);
//        return ApiResponseDto.readOk(list);
//    }
    @GetMapping("/list")
    public ApiResponseDto<Page<ProductResponseDto>> getProductListByStoreId(@RequestParam(required = false,defaultValue = "") String keyword,@RequestParam Long storeId,@PageableDefault(page = 0,size = 10, sort = {"productId", "createdAt"}, direction = Sort.Direction.DESC) Pageable pageable){
        return ApiResponseDto.readOk(productService.getProductListByStoreId(keyword,storeId,pageable));
    }
    @GetMapping("/search/es")
    public ApiResponseDto<Page<ProductResponseDto>> searchProducts(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "") String keyword,
            Pageable pageable
    ) {
        log.info("keyword :{}", keyword);
        List<Long> storeIds = remoteStoreService.getNearStoreIds(latitude, longitude).getData();
        log.info("storeIds :{}", storeIds);
        List<Long> productIds = productSearchService.nearStoreSearchProducts(keyword, storeIds);
        Page<ProductResponseDto> pagedProducts = productService.getProductsByProductIds(productIds, pageable);
        return ApiResponseDto.readOk(pagedProducts);
    }
    @GetMapping("/search/{storeId}")
    public ApiResponseDto<Page<ProductResponseDto>> searchProductsInStore(
            @RequestParam(defaultValue = "") String keyword,
            @PathVariable Long storeId,
            Pageable pageable
    ) {
        log.info("keyword :{}", keyword);
        List<Long> storeIds = List.of(storeId);
        log.info("storeIds :{}", storeIds);
        List<Long> productIds = productSearchService.nearStoreSearchProducts(keyword, storeIds);
        Page<ProductResponseDto> pagedProducts = productService.getProductsByProductIds(productIds, pageable);
        return ApiResponseDto.readOk(pagedProducts);
    }
    @GetMapping("/detail/{productId}")
    public ApiResponseDto<ProductResponseDto> getProductById(@PathVariable Long productId) {
        ProductResponseDto dto = productService.getProductById(productId);
        return ApiResponseDto.readOk(dto);
    }
    @PostMapping("/view/{productId}")
    public ApiResponseDto<String> productViewAdd(@PathVariable Long productId) {
        productService.productViewAdd(productId);
        return ApiResponseDto.defaultOk();
    }

    @GetMapping("/personalize/recommend")
    public ApiResponseDto<List<ProductResponseDto>> getUserRecommendProducts(){
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();
        List<ProductResponseDto> userRecommendProducts = productService.getUserRecommendProducts(userId);
        return ApiResponseDto.readOk(userRecommendProducts);
    }
}
