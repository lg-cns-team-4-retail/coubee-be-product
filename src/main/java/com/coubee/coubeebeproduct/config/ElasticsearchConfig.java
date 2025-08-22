package com.coubee.coubeebeproduct.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "elasticsearch.enable", havingValue = "true")
public class ElasticsearchConfig {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    /**
     * RestClient를 분리해서 close 되도록 관리 (컨테이너 종료 시 자원 해제)
     */
    @Bean(destroyMethod = "close")
    public RestClient restClient() {
        // host에 스킴이 없다고 가정하고 http 기본 사용
        // (만약 'http://...' 같이 스킴이 들어오면 HttpHost.create 사용)
        HttpHost httpHost = (host.startsWith("http://") || host.startsWith("https://"))
                ? HttpHost.create(host + (host.contains(":") ? "" : ":" + port))
                : new HttpHost(host, port, "http");

        return RestClient.builder(httpHost).build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}