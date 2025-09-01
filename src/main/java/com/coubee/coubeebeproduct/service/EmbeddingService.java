package com.coubee.coubeebeproduct.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://embedding-external:8089")
            .build();

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