package com.coubee.coubeebeproduct.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

//@Service
//@RequiredArgsConstructor
//public class EmbeddingService {
//    @Value("${embedding_url}")
//    private String embeddingUrl;
//
//    private final WebClient webClient = WebClient.builder()
////            .baseUrl("http://embedding-external:8089")
//            .baseUrl(embeddingUrl)
//            .build();
//
//    public List<Float> embed(String keyword) {
//        return webClient.post()
//                .uri("/embed")
//                .bodyValue(Map.of("keyword", keyword))
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<Map<String, List<Float>>>() {})
//                .block()
//                .get("vector");
//    }
//}

@Service
public class EmbeddingService {

    private final WebClient webClient;

    public EmbeddingService(@Value("${embedding_url}") String embeddingUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(embeddingUrl)
                .build();
    }

    public List<Float> embed(String keyword) {
        return webClient.post()
                .uri("/embed")
                .bodyValue(Map.of("keyword", keyword))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, List<Float>>>() {})
                .block()
                .get("vector");
    }
}