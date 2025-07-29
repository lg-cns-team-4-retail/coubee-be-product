package com.coubee.coubeebeproduct.domain.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Getter
@Setter
@Document(indexName = "products") // ES 인덱스명 (실제 사용 중인 이름으로 바꾸세요)
public class ProductDocument {
    @Id
    private Long productId;

    private String productName;

    private String description;
}