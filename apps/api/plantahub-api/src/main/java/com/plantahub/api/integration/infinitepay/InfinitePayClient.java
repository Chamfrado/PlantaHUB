package com.plantahub.api.integration.infinitepay;

import com.plantahub.api.integration.infinitepay.dto.CreateInfinitePayLinkRequest;
import com.plantahub.api.integration.infinitepay.dto.CreateInfinitePayLinkResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InfinitePayClient {

    private final RestClient restClient;
    private final InfinitePayProperties properties;

    public InfinitePayClient(InfinitePayProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .build();
    }

    public CreateInfinitePayLinkResponse createPaymentLink(CreateInfinitePayLinkRequest request) {
        return restClient.post()
                .uri("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CreateInfinitePayLinkResponse.class);
    }
}