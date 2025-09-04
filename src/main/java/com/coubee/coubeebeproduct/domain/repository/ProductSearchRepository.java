package com.coubee.coubeebeproduct.domain.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
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
            String fieldToSearch = keyword.length() == 1 ? "product_name.ngram_1" : "product_name.ngram";
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
            String fieldToSearch = keyword.length() == 1 ? "product_name.ngram_1" : "product_name.ngram";
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
    public List<Long> searchProductsV4SmartHybrid(String keyword, List<Long> storeIds) {
        try {
            final boolean singleChar = keyword != null && keyword.length() == 1;

            // 공통: store 필터
            Query storeFilter = Query.of(q -> q.terms(t -> t
                    .field("store_id")
                    .terms(ts -> ts.value(storeIds.stream().map(FieldValue::of).toList()))
            ));

            // ---- 텍스트 쿼리 구성 (v4용) ----
            Query textQuery = Query.of(q -> q.bool(b -> {
                b.must(storeFilter);
                // multi_match: ko_search(검색 시 동의어/불용어 반영)로 product_name 중심
                if (singleChar) {
                    // 1자: ngram_1을 강하게, product_name/ngram은 보조
                    b.should(sq -> sq.multiMatch(mm -> mm
                            .query(keyword)
                            .type(TextQueryType.BestFields)
                            .operator(Operator.Or)
                            .tieBreaker(0.3)
                            .fields("product_name^6", "product_name.ngram^2", "description^0.5")
                    ));
                    // 단일 글자라도 정확 구문 일치 약간 보강
                    b.should(sq -> sq.matchPhrase(mp -> mp.field("product_name").query(keyword).boost(4.0f)));
                } else {
                    // 2자 이상: ko_search의 동의어 효력 극대화 (product_name을 가장 강하게)
                    b.should(sq -> sq.multiMatch(mm -> mm
                            .query(keyword)
                            .type(TextQueryType.BestFields)  // ← 여기
                            .operator(Operator.Or)
                            .tieBreaker(0.3)
                            .fields("product_name^6", "product_name.ngram^2", "description^0.5")
                    ));

                    // 정확 구문 일치 강한 보정
                    b.should(sq -> sq.matchPhrase(mp -> mp.field("product_name").query(keyword).slop(1).boost(10.0f)));
                    // 오토컴플릿/접두 보정(공백 분절 prefix 매칭)
                    b.should(sq -> sq.matchBoolPrefix(mbp -> mbp.field("product_name").query(keyword).boost(3.0f)));
                }

                // 최소 한 개의 should 충족
                b.minimumShouldMatch("1");
                return b;
            }));

            SearchResponse<ProductDocument> response;

            if (singleChar) {
                // 1글자: 벡터 제외 + 높은 min_score로 잡음 억제
                response = esClient.search(s -> s
                                .index(INDEX)
                                .query(textQuery)
                                .minScore(15.0)         // 필요시 조정
                                .size(1000),
                        ProductDocument.class
                );
            } else {
                // 2글자 이상: 텍스트 + 벡터 가산
                long start = System.currentTimeMillis();
                List<Float> queryVector = embeddingService.embed(keyword);

                response = esClient.search(s -> s
                                .index(INDEX)
                                .query(q -> q.scriptScore(ss -> ss
                                        .query(textQuery)
                                        .script(sc -> sc
                                                // 텍스트 점수에 코사인 유사도(스케일 약간 가중) 가산
                                                .source("_score + 2.0 * cosineSimilarity(params.qv, 'vector_raw')")
                                                .params("qv", JsonData.of(queryVector))
                                        )
                                ))
                                .minScore(5.0)          // 필요시 조정
                                .size(1000),
                        ProductDocument.class
                );

                long end = System.currentTimeMillis();
                log.info("[V4 HYBRID] 검색 완료: {} ms (keyword: {}, storeIds: {})",
                        (end - start), keyword, storeIds.size());
            }

            return response.hits().hits().stream()
                    .map(Hit::id).filter(Objects::nonNull).map(Long::parseLong)
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch V4 Smart Hybrid query failed", e);
        }
    }
}
