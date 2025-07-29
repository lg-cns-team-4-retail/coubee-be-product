package com.coubee.coubeebeproduct.api.open;

import com.coubee.coubeebeproduct.common.dto.ApiResponseDto;
import com.coubee.coubeebeproduct.domain.Product;
import com.coubee.coubeebeproduct.domain.dto.ProductRegisterDto;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductUpdateDto;
import com.coubee.coubeebeproduct.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
    public ApiResponseDto<String> productRegister(@RequestBody ProductRegisterDto productRegisterDto) {
        productService.productRegister(productRegisterDto);
        return ApiResponseDto.defaultOk();
    }

    @PostMapping("/update")
    public ApiResponseDto<String> productUpdate(@RequestBody ProductUpdateDto productUpdateDto) {
        productService.productUpdate(productUpdateDto);
        return ApiResponseDto.defaultOk();
    }
    @PostMapping("/img/profile")
    public ApiResponseDto<String> productImgProfile(@RequestParam MultipartFile file) {
        String imgUrl = productService.productImgProfile(file);
        return ApiResponseDto.createOk(imgUrl);
    }

    @GetMapping("/list/{storeId}")
    public ApiResponseDto<Page<ProductResponseDto>> getMyProductList(@PathVariable Long storeId
            , @PageableDefault(size = 6, sort = "productId", direction = Sort.Direction.ASC) Pageable pageable){
        Page<ProductResponseDto> productPage = productService.getMyProductList(storeId,pageable);
        return ApiResponseDto.readOk(productPage);
    }
}
