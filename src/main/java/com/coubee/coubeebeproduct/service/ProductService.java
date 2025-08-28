package com.coubee.coubeebeproduct.service;

import com.coubee.coubeebeproduct.common.exception.NotFound;
import com.coubee.coubeebeproduct.common.web.context.GatewayRequestHeaderUtils;
import com.coubee.coubeebeproduct.domain.ItemRecommend;
import com.coubee.coubeebeproduct.domain.Product;
import com.coubee.coubeebeproduct.domain.ProductStatus;
import com.coubee.coubeebeproduct.domain.ProductViewRecord;
import com.coubee.coubeebeproduct.domain.dto.ProductRegisterDto;
import com.coubee.coubeebeproduct.domain.dto.ProductResponseDto;
import com.coubee.coubeebeproduct.domain.dto.ProductUpdateDto;
import com.coubee.coubeebeproduct.domain.mapper.ProductMapper;
import com.coubee.coubeebeproduct.domain.repository.ItemRecommendRepository;
import com.coubee.coubeebeproduct.domain.repository.ProductRepository;
import com.coubee.coubeebeproduct.domain.repository.ProductViewRecordRepository;
import com.coubee.coubeebeproduct.event.producer.KafkaMessageProducer;
import com.coubee.coubeebeproduct.event.producer.message.ProductEventMessage;
import com.coubee.coubeebeproduct.util.FileUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.StoreManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductViewRecordRepository productViewRecordRepository;
    private final ItemRecommendRepository itemRecommendRepository;

    private final FileUploader fileUploader;
    private final KafkaMessageProducer kafkaMessageProducer;

    @Transactional
    public ProductResponseDto productRegister(ProductRegisterDto productRegisterDto) {
        Product newProduct = productRepository.save(ProductMapper.toEntity(productRegisterDto));
        log.info("상품 생성 완료");
        kafkaMessageProducer.send(
                ProductEventMessage.Topic,
                ProductEventMessage.fromEntity("Create", newProduct)
        );
        log.info("상품 생성후 카프카 메세지 프로듀스 완료");
        return ProductMapper.fromEntity(newProduct);
    }

    @Transactional
    public ProductResponseDto productUpdate(ProductUpdateDto productUpdateDto) {
        Product product = productRepository.findById(productUpdateDto.getProductId())
                .orElseThrow(() -> new NotFound("해당 상품이 존재하지 않습니다"));
        String beforeName = product.getProductName();
        String beforeDesc = product.getDescription();
        product.updateProduct(productUpdateDto);
        String afterName = product.getProductName();
        String afterDesc = product.getDescription();
        boolean nameChanged = !Objects.equals(beforeName, afterName);
        boolean descChanged = !Objects.equals(beforeDesc, afterDesc);
        boolean changedForEvent = nameChanged || descChanged;
        log.info("상품 수정 완료");
        if(changedForEvent) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            kafkaMessageProducer.send(
                                    ProductEventMessage.Topic,
                                    ProductEventMessage.fromEntity("Update", product)
                            );
                            log.info("상품 수정 후(커밋 완료) 카프카 메시지 프로듀스 완료");
                        }
                    }
            );
        }
        log.info("상품 수정 완료: id={}, name:{} -> {}, descChanged={}",
                product.getProductId(), beforeName, afterName, descChanged);
        return ProductMapper.fromEntity(productRepository.save(product));
    }
    @Transactional
    public void productDelete(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(()->new NotFound("해당 상품이 존재하지 않습니다"));
        productViewRecordRepository.deleteByProduct(product);
        product.setStatus(ProductStatus.DELETED);
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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFound("해당 상품이 존재하지 않습니다."));
//        try {
//            userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();
//        } catch (NotFound e) {
//            // 로그인 안 된 상태로 간주 (권장: 별도 Unauthenticated 예외 or Optional API)
//        }
//        productViewRecordRepository.save(ProductViewRecord.builder().product(product).userId(userId).build());
        return ProductMapper.fromEntity(product);
    }
    public void productViewAdd(Long productId){
        Long userId = 0L;
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFound("해당 상품이 존재하지 않습니다."));
        try {
            String userIdStr = GatewayRequestHeaderUtils.getUserId();
            if(userIdStr!=null){
                userId = Long.parseLong(userIdStr);
            }
        } catch (NotFound e) {
            // 로그인 안 된 상태로 간주 (권장: 별도 Unauthenticated 예외 or Optional API)
        }
        productViewRecordRepository.save(ProductViewRecord.builder().product(product).userId(userId).build());
    }

    //// 일반 유저 기능
//    public List<ProductResponseDto> getProductsByProductIds(List<Long> productIds){
//        return productRepository.findByProductIdInOrderByProductIdDesc(productIds).stream().map(ProductMapper::fromEntity).toList();
//    }

    public Page<ProductResponseDto> getProductsByProductIds(List<Long> productIds, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), productIds.size());
        List<Long> pagedIds = productIds.subList(start, end);
        List<Product> products = productRepository.findByProductIdIn(pagedIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
        List<Product> sorted = pagedIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .toList();
        List<ProductResponseDto> dtoList = sorted.stream()
                .map(ProductMapper::fromEntity)
                .toList();
        return new PageImpl<>(dtoList, pageable, productIds.size());
    }

    public Page<ProductResponseDto> getProductListByStoreId(String keyword,Long storeId,Pageable pageable){
        return productRepository.findAllByProductNameContainingAndStoreId(keyword,storeId, pageable)
                .map(ProductMapper::fromEntity);
    }


    public List<ProductResponseDto> getUserRecommendProducts(Long userId) {
        return itemRecommendRepository.findById(userId)
                .map(itemRecommend -> {
                    String raw = itemRecommend.getRecommendItems();
                    String cleaned = raw.replace("[", "").replace("]", "");
                    List<Long> productIds = Arrays.stream(cleaned.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Long::valueOf)
                            .toList();
                    return productRepository.findAllById(productIds).stream()
                            .map(ProductMapper::fromEntity)
                            .toList();
                })
                .orElseGet(List::of);
    }
}
