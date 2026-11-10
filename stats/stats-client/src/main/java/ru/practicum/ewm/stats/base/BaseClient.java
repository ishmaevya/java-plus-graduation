package ru.practicum.ewm.stats.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.ewm.stats.exception.StatsServerUnavailable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Slf4j
public class BaseClient {

    protected final RestTemplate rest;

    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String serverId;

    protected BaseClient(RestTemplateBuilder builder,
                         DiscoveryClient discoveryClient,
                         RetryTemplate retryTemplate,
                         String serverId) {
        this.rest = builder
                .requestFactory(() -> new JdkClientHttpRequestFactory())
                .build();
        this.discoveryClient = discoveryClient;
        this.retryTemplate = retryTemplate;
        this.serverId = serverId;
    }

    protected <T> T execute(Function<String, T> request) {
        return retryTemplate.execute(context -> {
            String baseUrl = resolveBaseUrl();
            try {
                return request.apply(baseUrl);
            } catch (ResourceAccessException e) {
                log.warn("Failed to call {}: {}", serverId, e.getMessage());
                throw new StatsServerUnavailable("Stats server is unavailable", e);
            }
        });
    }

    private String resolveBaseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(serverId);
        if (instances == null || instances.isEmpty()) {
            throw new StatsServerUnavailable("No instances of " + serverId + " found in Eureka");
        }
        ServiceInstance instance = instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
        return instance.getUri().toString();
    }
}
