package com.coubee.coubeebeproduct.service;

import com.coubee.coubeebeproduct.common.exception.NotFound;
import com.coubee.coubeebeproduct.domain.Product;
import com.coubee.coubeebeproduct.domain.dto.ProductRegisterDto;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductUpdateDto;
import com.coubee.coubeebeproduct.domain.repository.ProductRepository;
import com.coubee.coubeebeproduct.util.FileUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;

    private final FileUploader fileUploader;

    @Transactional
    public void productRegister(ProductRegisterDto productRegisterDto) {
        Product newProduct = Product.builder()
                .productName(productRegisterDto.getProductName())
                .productImg(productRegisterDto.getProductImg())
                .originPrice(productRegisterDto.getOriginPrice())
                .salePrice(productRegisterDto.getSalePrice())
                .description(productRegisterDto.getDescription())
                .stock(productRegisterDto.getStock())
                .storeId(productRegisterDto.getStoreId())
                .build();
        productRepository.save(newProduct);
    }

    @Transactional
    public void productUpdate(ProductUpdateDto productUpdateDto) {
        Product product = productRepository.findById(productUpdateDto.getProductId())
                .orElseThrow(() -> new NotFound("해당 상품이 존재하지 않습니다"));
        product.updateProduct(productUpdateDto);
        productRepository.save(product);
    }

    public Page<ProductResponseDto> getMyProductList(Long storeId, Pageable pageable){
        Page<Product> products = productRepository.findAllByStoreId(storeId, pageable);
        return products.map(ProductResponseDto::from);
    }
    public String productImgProfile(MultipartFile file){
        return fileUploader.upload(file, "product/profile");
    }
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFound("해당 상품이 존재하지 않습니다."));
        return ProductResponseDto.from(product);
    }

}
