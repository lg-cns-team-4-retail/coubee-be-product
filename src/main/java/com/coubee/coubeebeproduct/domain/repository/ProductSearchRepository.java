package com.coubee.coubeebeproduct.domain.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.coubee.coubeebeproduct.domain.dto.ProductSearchResponse;
import com.coubee.coubeebeproduct.domain.elasticsearch.ProductDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;

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

    public List<Long> nearStoreSearchProducts(String keyword, List<Long> storeIds) {
        try {
            List<FieldValue> storeIdValues = storeIds.stream()
                    .map(FieldValue::of)
                    .toList();

            // ✅ FunctionScore 리스트 정의
            List<FunctionScore> functionScores = Arrays.asList(
                    FunctionScore.of(fn -> fn
                            .filter(f -> f.match(m -> m
                                    .field("product_name.edge")
                                    .query(keyword)
                            ))
                            .weight(30.0)
                    ),
                    FunctionScore.of(fn -> fn
                            .filter(f -> f.wildcard(w -> w
                                    .field("product_name.keyword")
                                    .value("*" + keyword + "*")
                            ))
                            .weight(50.0)
                    )
            );

            SearchResponse<ProductDocument> response = esClient.search(s -> s
                            .index("products")
                            .query(q -> q
                                    .functionScore(fs -> fs
                                            .query(fq -> fq
                                                    .bool(b -> b
                                                            .must(m -> m
                                                                    .terms(t -> t
                                                                            .field("store_id")
                                                                            .terms(ts -> ts.value(storeIdValues))
                                                                    )
                                                            )
                                                            .should(sq1 -> sq1
                                                                    .matchPhrase(mp -> mp
                                                                            .field("product_name")
                                                                            .query(keyword)
                                                                            .boost(10.0f)
                                                                    )
                                                            )
                                                            .should(sq2 -> sq2
                                                                    .match(m -> m
                                                                            .field("product_name")
                                                                            .query(keyword)
                                                                            .fuzziness("AUTO")
                                                                            .boost(2.0f)
                                                                    )
                                                            )
                                                            .should(sq3 -> sq3
                                                                    .match(m -> m
                                                                            .field("description")
                                                                            .query(keyword)
                                                                            .fuzziness("AUTO")
                                                                            .boost(1.0f)
                                                                    )
                                                            )
                                                    )
                                            )
                                            .functions(functionScores)
                                            .scoreMode(FunctionScoreMode.Sum)
                                            .boostMode(FunctionBoostMode.Sum)
                                    )
                            )
                            .size(1000),
                    ProductDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::id).filter(Objects::nonNull).map(Long::parseLong)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch advanced query failed", e);
        }
    }
}