package com.coubee.coubeebeproduct.domain.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.coubee.coubeebeproduct.domain.elasticsearch.ProductDocument;
import com.coubee.coubeebeproduct.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductSearchRepository {

    private final ElasticsearchClient esClient;
    private final EmbeddingService embeddingService;
    private static final String INDEX = "products";


    public List<Long> nearStoreSearchProductsKnn(String keyword, List<Long> storeIds) {
        try {
            long start = System.currentTimeMillis();
            List<Float> queryVector = embeddingService.embed(keyword);
            log.info("queryVector: {}", queryVector);

            SearchResponse<ProductDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .knn(knn -> knn
                                    .field("vector_knn")   // ✅ knn 전용 필드
                                    .queryVector(queryVector)
                                    .k(1000)
                                    .numCandidates(1000)
                                    .filter(f -> f.terms(t -> t
                                            .field("store_id")
                                            .terms(ts -> ts.value(
                                                    storeIds.stream().map(FieldValue::of).toList()
                                            ))
                                    ))
                            ),
                    ProductDocument.class
            );
            long end = System.currentTimeMillis();
            log.info("[KNN] 검색 완료: {} ms (keyword: {}, storeIds: {})",
                    (end - start), keyword, storeIds.size());
            return response.hits().hits().stream()
                    .map(Hit::id).filter(Objects::nonNull).map(Long::parseLong)
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch KNN query failed", e);
        }
    }

    public List<Long> nearStoreSearchProductsHybrid(String keyword, List<Long> storeIds) {
        try {
            long start = System.currentTimeMillis();
            List<Float> queryVector = embeddingService.embed(keyword);
            log.info("queryVector: {}", queryVector);

            SearchResponse<ProductDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .query(q -> q
                                    .scriptScore(ss -> ss
                                            .query(innerQ -> innerQ
                                                    .bool(b -> b
                                                            // ✅ storeId 필터
                                                            .must(m -> m.terms(t -> t
                                                                    .field("store_id")
                                                                    .terms(ts -> ts.value(storeIds.stream()
                                                                            .map(FieldValue::of)
                                                                            .toList()))
                                                            ))
                                                            // ✅ vector_raw 있는 문서만
                                                            .must(m -> m.exists(e -> e.field("vector_raw")))
                                                            .should(sq1 -> sq1.matchPhrase(mp -> mp
                                                                    .field("product_name")
                                                                    .query(keyword)
                                                                    .boost(10.0f)
                                                            ))
                                                            .should(sq2 -> sq2.match(m -> m
                                                                    .field("product_name")
                                                                    .query(keyword)
                                                                    .fuzziness("AUTO")
                                                                    .boost(2.0f)
                                                            ))
                                                            .should(sq3 -> sq3.match(m -> m
                                                                    .field("description")
                                                                    .query(keyword)
                                                                    .fuzziness("AUTO")
                                                                    .boost(1.0f)
                                                            ))
                                                    )
                                            )
                                            .script(sc -> sc
                                                    .source("_score + cosineSimilarity(params.query_vector, 'vector_raw')")
                                                    .params("query_vector", JsonData.of(queryVector))
                                            )
                                    )
                            )
                            .size(1000),
                    ProductDocument.class
            );
            long end = System.currentTimeMillis(); // 종료 시간 기록
            log.info("[HYBRID] 검색 완료: {} ms (keyword: {}, storeIds: {})",
                    (end - start), keyword, storeIds.size());
            return response.hits().hits().stream()
                    .map(Hit::id).filter(Objects::nonNull).map(Long::parseLong)
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch Hybrid query failed", e);
        }
    }


    public List<Long> defaultSearch(String keyword, List<Long> storeIds) {
        try {
            SearchResponse<ProductDocument> response = esClient.search(s -> s
                            .index("products")
                            .query(q -> q
                                    .functionScore(fs -> fs
                                            .query(innerQ -> innerQ
                                                    .bool(b -> b
                                                            .must(m -> m.terms(t -> t
                                                                    .field("store_id")
                                                                    .terms(ts -> ts.value(storeIds.stream()
                                                                            .map(FieldValue::of)
                                                                            .toList()))
                                                            ))
                                                            .should(sh -> sh.matchPhrase(mp -> mp
                                                                    .field("product_name")
                                                                    .query(keyword)
                                                                    .boost(10.0f)
                                                            ))
                                                            .should(sh -> sh.match(m -> m
                                                                    .field("product_name")
                                                                    .query(keyword)
                                                                    .fuzziness("AUTO")
                                                                    .boost(2.0f)
                                                            ))
                                                            .should(sh -> sh.match(m -> m
                                                                    .field("description")
                                                                    .query(keyword)
                                                                    .fuzziness("AUTO")
                                                                    .boost(1.0f)
                                                            ))
                                                    )
                                            )
                                            .functions(f -> f
                                                    .filter(fq -> fq.match(m -> m
                                                            .field("product_name.edge")
                                                            .query(keyword)
                                                    ))
                                                    .weight(30.0)
                                            )
                                            .functions(f -> f
                                                    .filter(fq -> fq.wildcard(wc -> wc
                                                            .field("product_name.keyword")
                                                            .value(keyword)
                                                    ))
                                                    .weight(50.0)
                                            )
                                            .scoreMode(FunctionScoreMode.Sum)
                                            .boostMode(FunctionBoostMode.Sum)
                                    )
                            ),
                    ProductDocument.class
            );
            return response.hits().hits().stream()
                    .map(Hit::id).filter(Objects::nonNull).map(Long::parseLong)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch Hybrid query failed", e);
        }
    }
}