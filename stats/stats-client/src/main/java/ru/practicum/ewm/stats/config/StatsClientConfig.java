package ru.practicum.ewm.stats.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import ru.practicum.ewm.stats.exception.StatsServerUnavailable;

@Configuration
public class StatsClientConfig {

    @Bean
    public RetryTemplate statsRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .fixedBackoff(1000)
                .retryOn(StatsServerUnavailable.class)
                .build();
    }
}
