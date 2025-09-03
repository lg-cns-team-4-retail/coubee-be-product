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

    @Bean(destroyMethod = "close")
    public RestClient restClient() {
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