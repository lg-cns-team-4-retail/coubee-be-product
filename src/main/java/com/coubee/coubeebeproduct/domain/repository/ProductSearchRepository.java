package com.coubee.coubeebeproduct.domain.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
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
    private static final String INDEX = "products_alias";

    /**
     * 공통 BoolQuery 빌더
     */
    private Query buildQuery(String fieldToSearch, String keyword, List<Long> storeIds, boolean useFuzziness) {
        return Query.of(q -> q
                .bool(b -> {
                    b.must(m -> m.terms(t -> t
                            .field("store_id")
                            .terms(ts -> ts.value(storeIds.stream().map(FieldValue::of).toList()))
                    ));
                    b.must(m -> m.exists(e -> e.field("vector_raw")));

                    b.should(sq1 -> sq1.matchPhrase(mp -> mp
                            .field(fieldToSearch)
                            .query(keyword)
                            .boost(10.0f)
                    ));

                    b.should(sq2 -> sq2.match(m -> {
                        m.field(fieldToSearch)
                                .query(keyword)
                                .boost(2.0f);
                        if (useFuzziness) {
                            m.fuzziness("AUTO");
                        }
                        return m;
                    }));
                    if (keyword.length() > 1) {
                        b.should(sq3 -> sq3.matchPhrase(m -> m
                                .field("description.ngram")
                                .query(keyword)
                                .boost(0.1f)
                        ));
                    }
                    return b;
                })
        );
    }
    /**
     * KNN Search
     */
    public List<Long> nearStoreSearchProductsKnn(String keyword, List<Long> storeIds) {
        try {
            long start = System.currentTimeMillis();
            List<Float> queryVector = embeddingService.embed(keyword);

            SearchResponse<ProductDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .minScore(0.82)
                            .knn(knn -> knn
                                    .field("vector_knn")
                                    .queryVector(queryVector)
                                    .k(500)
                                    .numCandidates(8000)
                                    .filter(f -> f.terms(t -> t
                                            .field("store_id")
                                            .terms(ts -> ts.value(storeIds.stream().map(FieldValue::of).toList()))
                                    ))
                            ).size(1000),
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

    /**
     * Hybrid Search
     */
    public List<Long> nearStoreSearchProductsHybrid(String keyword, List<Long> storeIds) {
        try {
            String fieldToSearch = keyword.length() == 1 ? "product_name.ngram_1" : "product_name";
            boolean useFuzziness = keyword.length() > 1;

            long start = System.currentTimeMillis();
            List<Float> queryVector = embeddingService.embed(keyword);
            log.info("queryVector : {}", queryVector);
            SearchResponse<ProductDocument> response;

            if (keyword.length() == 1) {
                //한 글자 검색어는 vector score 제외 (scriptScore 미사용)
                response = esClient.search(s -> s
                                .index(INDEX)
                                .minScore(15.0) // 필요시 조정
                                .query(buildQuery(fieldToSearch, keyword, storeIds, useFuzziness))
                                .size(1000),
                        ProductDocument.class
                );
            } else {
                //두 글자 이상은 hybrid (text + vector)
                response = esClient.search(s -> s
                                .index(INDEX)
                                .minScore(5.0)
                                .query(q -> q
                                        .scriptScore(ss -> ss
                                                .query(buildQuery(fieldToSearch, keyword, storeIds, useFuzziness))
                                                .script(sc -> sc
                                                        .source("_score + cosineSimilarity(params.query_vector, 'vector_raw')")
                                                        .params("query_vector", JsonData.of(queryVector))
                                                )
                                        )
                                )
                                .size(1000),
                        ProductDocument.class
                );
            }

            long end = System.currentTimeMillis();
            log.info("[HYBRID] 검색 완료: {} ms (keyword: {}, storeIds: {})",
                    (end - start), keyword, storeIds.size());

            return response.hits().hits().stream()
                    .map(Hit::id).filter(Objects::nonNull).map(Long::parseLong)
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch Hybrid query failed", e);
        }
    }

    /**
     * Default Search
     */
    public List<Long> defaultSearch(String keyword, List<Long> storeIds) {
        try {
            String fieldToSearch = keyword.length() == 1 ? "product_name.ngram_1" : "product_name";
            boolean useFuzziness = keyword.length() > 1;

            SearchResponse<ProductDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .query(q -> q
                                    .functionScore(fs -> fs
                                            .query(buildQuery(fieldToSearch, keyword, storeIds, useFuzziness))
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
                            )
                            .size(1000)
                            .minScore(5.0),
                    ProductDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::id).filter(Objects::nonNull).map(Long::parseLong)
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch Default query failed", e);
        }
    }
}
