package com.coubee.coubeebeproduct.domain.elasticsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "products")
public class ProductDocument {
    @Id
    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("product_name")
    private String productName;

    private String description;

    @JsonProperty("store_id")
    private Long storeId;

    @JsonProperty("total_count")
    private Integer totalCount;
}