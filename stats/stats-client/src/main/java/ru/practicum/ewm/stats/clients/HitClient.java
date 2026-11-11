package ru.practicum.ewm.stats.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.base.BaseClient;
import ru.practicum.ewm.stats.dto.StatHitRequest;

import static ru.practicum.ewm.stats.base.ClientConstants.API_PREFIX_HIT;

@Slf4j
@Component
public class HitClient extends BaseClient {

    public HitClient(RestTemplateBuilder builder,
                     DiscoveryClient discoveryClient,
                     RetryTemplate retryTemplate,
                     @Value("${stats-server.id:stats-server}") String serverId) {
        super(builder, discoveryClient, retryTemplate, serverId);
    }

    public void saveHit(StatHitRequest request) {
        log.debug("Post saveHit request: {}", request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StatHitRequest> entity = new HttpEntity<>(request, headers);

        execute(baseUrl -> rest.postForEntity(baseUrl + API_PREFIX_HIT, entity, Void.class));
    }
}
