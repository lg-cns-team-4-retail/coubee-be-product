package com.coubee.coubeebeproduct.api.open;

import com.coubee.coubeebeproduct.common.dto.ApiResponseDto;
import com.coubee.coubeebeproduct.domain.Product;
import com.coubee.coubeebeproduct.domain.dto.ProductRegisterDto;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductUpdateDto;
import com.coubee.coubeebeproduct.domain.mapper.ProductMapper;
import com.coubee.coubeebeproduct.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(value = "/api/product/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductService productService;

    /* todo :
      1. 상품등록
      2. 상품 수정
      3. 상품 이미지 저장
   */
    @PostMapping("/register")
    public ApiResponseDto<ProductResponseDto> productRegister(@RequestBody ProductRegisterDto productRegisterDto) {
        return ApiResponseDto.createOk(productService.productRegister(productRegisterDto));
    }

    @PostMapping("/update")
    public ApiResponseDto<ProductResponseDto> productUpdate(@RequestBody ProductUpdateDto productUpdateDto) {
        return ApiResponseDto.createOk(productService.productUpdate(productUpdateDto));
    }

    @PostMapping("/delete/{productId}")
    public ApiResponseDto<String> productDelete(@PathVariable Long productId) {
        productService.productDelete(productId);
        return ApiResponseDto.defaultOk();
    }

    @PostMapping("/img/profile")
    public ApiResponseDto<String> productImgProfile(@RequestParam MultipartFile file) {
        String imgUrl = productService.productImgProfile(file);
        return ApiResponseDto.createOk(imgUrl);
    }

    @GetMapping("/list/{storeId}")
    public ApiResponseDto<Page<ProductResponseDto>> getMyProductList(@PathVariable Long storeId,
                                                                     @RequestParam(defaultValue = "") String keyword
            , @PageableDefault(size = 6, sort = "productId") Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.getOrderFor("productId") == null) {
            sort = sort.and(Sort.by("productId").ascending());
        }
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<ProductResponseDto> productPage = productService.getMyProductList(storeId, keyword, sortedPageable);
        return ApiResponseDto.readOk(productPage);
    }

    @GetMapping("/detail/{productId}")
    public ApiResponseDto<ProductResponseDto> getProductById(@PathVariable Long productId) {
        ProductResponseDto dto = productService.getProductById(productId);
        return ApiResponseDto.readOk(dto);
    }

    /* todo:
        1. update product
        2. delete product(soft)
        3. update stock
        4. product category(personalize 에서 리스트로 받지 못하기때문에 분석에는 사용되지 않음 그냥 사용자 뷰에 띄우는 용"
        5. product view record -> personalize 분석에 사용
     */
}
