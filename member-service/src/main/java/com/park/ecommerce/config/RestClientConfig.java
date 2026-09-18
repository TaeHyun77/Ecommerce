package com.park.ecommerce.config;

import com.park.ecommerce.exception.MemberErrorCode;
import com.park.ecommerce.exception.MemberException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient orderServiceRestClient(
            RestClient.Builder builder,
            @Value("${external.order-service.base-url}") String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory())
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            throw new MemberException(MemberErrorCode.ORDER_SERVICE_UNAVAILABLE);
                        }
                )
                .build();
    }

    private ClientHttpRequestFactory requestFactory() {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(5));
        return ClientHttpRequestFactoryBuilder.detect().build(settings);
    }
}
