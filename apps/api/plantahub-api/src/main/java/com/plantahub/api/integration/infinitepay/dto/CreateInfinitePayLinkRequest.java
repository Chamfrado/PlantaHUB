package com.plantahub.api.integration.infinitepay.dto;

import java.util.List;

public record CreateInfinitePayLinkRequest(
        String handle,
        String redirect_url,
        String webhook_url,
        String order_nsu,
        List<InfinitePayItemDTO> items,
        InfinitePayCustomerDTO customer
) {
}