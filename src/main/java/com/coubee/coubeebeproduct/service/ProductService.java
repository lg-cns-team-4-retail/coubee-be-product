package com.coubee.coubeebeproduct.service;

import com.coubee.coubeebeproduct.common.exception.NotFound;
import com.coubee.coubeebeproduct.common.web.context.GatewayRequestHeaderUtils;
import com.coubee.coubeebeproduct.domain.Product;
import com.coubee.coubeebeproduct.domain.ProductViewRecord;
import com.coubee.coubeebeproduct.domain.dto.ProductRegisterDto;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductUpdateDto;
import com.coubee.coubeebeproduct.domain.mapper.ProductMapper;
import com.coubee.coubeebeproduct.domain.repository.ProductRepository;
import com.coubee.coubeebeproduct.domain.repository.ProductViewRecordRepository;
import com.coubee.coubeebeproduct.util.FileUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductViewRecordRepository productViewRecordRepository;

    private final FileUploader fileUploader;

    @Transactional
    public void productRegister(ProductRegisterDto productRegisterDto) {
        productRepository.save(ProductMapper.toEntity(productRegisterDto));
    }

    @Transactional
    public void productUpdate(ProductUpdateDto productUpdateDto) {
        Product product = productRepository.findById(productUpdateDto.getProductId())
                .orElseThrow(() -> new NotFound("해당 상품이 존재하지 않습니다"));
        product.updateProduct(productUpdateDto);
        productRepository.save(product);
    }
    @Transactional
    public void productDelete(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(()->new NotFound("해당 상품이 존재하지 않습니다"));
        productViewRecordRepository.deleteByProduct(product);
        productRepository.delete(product);
    }

    public Page<ProductResponseDto> getMyProductList(Long storeId,String keyword,Pageable pageable){
        if (keyword == null) keyword = "";
        Page<Product> products = productRepository.searchByKeyword(storeId,keyword,pageable);
        return products.map(ProductMapper::fromEntity);
    }
    public String productImgProfile(MultipartFile file){
        return fileUploader.upload(file, "product/profile");
    }
    public ProductResponseDto getProductById(Long id) {
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFound("해당 상품이 존재하지 않습니다."));
        increaseViewCountIfExistsElseCreate(product,userId);
        return ProductMapper.fromEntity(product);
    }

    @Transactional
    public void increaseViewCountIfExistsElseCreate(Product product, Long userId) {
        Optional<ProductViewRecord> pvr = productViewRecordRepository.findByProductIdAndUserId(product.getProductId(),userId);
        pvr.ifPresentOrElse(
                ProductViewRecord::incrementViewCount,
                () -> productViewRecordRepository.save(ProductViewRecord.builder().product(product).userId(userId).build())
        );
    }

}
