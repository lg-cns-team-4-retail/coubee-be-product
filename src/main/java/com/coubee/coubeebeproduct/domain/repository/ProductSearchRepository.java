package com.coubee.coubeebeproduct.domain.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.coubee.coubeebeproduct.domain.dto.ProductSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {

    private final ElasticsearchClient esClient;
    private static final String INDEX = "products";

    public List<ProductSearchResponse> hybridSearch(String keyword) {
        try {
            // Step 1: BM25로 대표 상품 검색
            SearchResponse<Map> keywordSearch = esClient.search(s -> s
                    .index(INDEX)
                    .size(1)
                    .query(q -> q
                            .match(m -> m
                                    .field("product_name")
                                    .query(keyword)
                            )
                    ), Map.class);

            List<Hit<Map>> hits = keywordSearch.hits().hits();
            if (hits.isEmpty()) return Collections.emptyList();

            Map<String, Object> topSource = hits.get(0).source();
            String productName = (String) topSource.get("product_name");
            List<Double> vector = (List<Double>) topSource.get("vector");

            // Step 2: 벡터 유사도 기반 검색
            SearchResponse<Map> vectorSearch = esClient.search(s -> s
                    .index(INDEX)
                    .size(10)
                    .source(builder -> builder
                            .filter(f -> f
                                    .excludes("vector")
                            )
                    )
                    .query(q -> q
                            .scriptScore(ss -> ss
                                    .query(Query.of(m -> m.matchAll(ma -> ma)))
                                    .script(script -> script
                                            .source("cosineSimilarity(params.query_vector, 'vector') + 1.0")
                                            .params(Map.of(
                                                    "query_vector", JsonData.of(vector)
                                            ))
                                    )
                            )
                    ), Map.class);

            return vectorSearch.hits().hits().stream()
                    .map(hit -> {
                        Map<String, Object> doc = hit.source();
                        return ProductSearchResponse.builder()
                                .productName((String) doc.get("product_name"))
                                .productId((Integer) doc.get("product_id"))
                                .score(hit.score())
                                .build();
                    })
                    .toList();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Elasticsearch search failed", e);
        }
    }
}