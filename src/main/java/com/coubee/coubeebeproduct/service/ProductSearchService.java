package com.coubee.coubeebeproduct.service;

import com.coubee.coubeebeproduct.domain.dto.ProductSearchResponse;
import com.coubee.coubeebeproduct.domain.elasticsearch.ProductDocument;
import com.coubee.coubeebeproduct.domain.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchRepository productSearchRepository;

    public List<ProductDocument> searchByName(String keyword, int page, int size) {
        Criteria criteria = Criteria.where("productName").matches(keyword); // 형태소 분석 기반 연관검색
        Pageable pageable = PageRequest.of(page, size);

        CriteriaQuery query = new CriteriaQuery(criteria, pageable);

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
    }

    public List<ProductSearchResponse> hybridSearch(String keyword) {
        return productSearchRepository.hybridSearch(keyword);
    }
}