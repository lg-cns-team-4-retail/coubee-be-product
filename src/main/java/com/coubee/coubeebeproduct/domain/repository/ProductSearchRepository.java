package com.coubee.coubeebeproduct.domain.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.coubee.coubeebeproduct.domain.elasticsearch.ProductDocument;
import com.coubee.coubeebeproduct.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {

    private final ElasticsearchClient esClient;
    private final EmbeddingService embeddingService;
    private static final String INDEX = "products";

    public List<Long> nearStoreSearchProducts(String keyword, List<Long> storeIds) {
        try {
            List<FieldValue> storeIdValues = storeIds.stream()
                    .map(FieldValue::of)
                    .toList();

            // FastAPI 호출해서 벡터 가져오기
            List<Float> queryVector = embeddingService.embed(keyword);

            // FunctionScore 리스트 정의 (기존 그대로)
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
                            .index(INDEX)
                            .query(q -> q
                                    .scriptScore(ss -> ss
                                            .query(innerQ -> innerQ
                                                    .functionScore(fs -> fs
                                                            .query(fq -> fq.bool(b -> b
                                                                    .must(m -> m
                                                                            .terms(t -> t
                                                                                    .field("store_id")
                                                                                    .terms(ts -> ts.value(storeIdValues))
                                                                            )
                                                                    )
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
                                                            ))
                                                            .functions(functionScores)
                                                            .scoreMode(FunctionScoreMode.Sum)
                                                            .boostMode(FunctionBoostMode.Sum)
                                                    )
                                            )
                                            // 벡터 점수 반영
                                            .script(sc -> sc
                                                    .source("cosineSimilarity(params.query_vector, 'vector') + 1.0")
                                                    .params("query_vector", JsonData.of(queryVector))
                                            )
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