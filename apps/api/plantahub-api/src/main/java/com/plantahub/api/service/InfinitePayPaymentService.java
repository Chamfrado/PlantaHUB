package com.plantahub.api.service;

import com.plantahub.api.domain.orders.Order;
import com.plantahub.api.domain.orders.OrderItem;
import com.plantahub.api.domain.orders.OrderItemSelection;
import com.plantahub.api.integration.infinitepay.InfinitePayClient;
import com.plantahub.api.integration.infinitepay.InfinitePayProperties;
import com.plantahub.api.integration.infinitepay.dto.CreateInfinitePayLinkRequest;
import com.plantahub.api.integration.infinitepay.dto.CreateInfinitePayLinkResponse;
import com.plantahub.api.integration.infinitepay.dto.InfinitePayCustomerDTO;
import com.plantahub.api.integration.infinitepay.dto.InfinitePayItemDTO;
import com.plantahub.api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class InfinitePayPaymentService {

    private final InfinitePayClient infinitePayClient;
    private final InfinitePayProperties properties;
    private final OrderRepository orderRepository;

    public InfinitePayPaymentService(
            InfinitePayClient infinitePayClient,
            InfinitePayProperties properties,
            OrderRepository orderRepository
    ) {
        this.infinitePayClient = infinitePayClient;
        this.properties = properties;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public String createPaymentLinkForOrder(Order order) {
        List<InfinitePayItemDTO> items = order.getItems().stream()
                .map(item -> new InfinitePayItemDTO(
                        item.getQuantity(),
                        item.getUnitPriceCents().longValue(),
                        buildItemDescription(item)
                ))
                .toList();

        InfinitePayCustomerDTO customer = new InfinitePayCustomerDTO(
                resolveCustomerName(order),
                order.getUser().getEmail(),
                order.getUser().getPhoneNumber()
        );

        CreateInfinitePayLinkRequest request = new CreateInfinitePayLinkRequest(
                properties.handle(),
                properties.redirectUrl(),
                properties.webhookUrl(),
                order.getId().toString(),
                items,
                customer
        );

        CreateInfinitePayLinkResponse response = infinitePayClient.createPaymentLink(request);

        if (response == null || response.url() == null || response.url().isBlank()) {
            throw new IllegalStateException("infinitepay_payment_url_not_returned");
        }

        order.setPaymentProvider("INFINITEPAY");
        order.setPaymentUrl(response.url());

        orderRepository.save(order);

        return response.url();
    }

    private String resolveCustomerName(Order order) {
        String fullName = order.getUser().getFullName();

        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }

        return order.getUser().getEmail();
    }

    private String buildItemDescription(OrderItem item) {
        String productName = item.getProduct().getName();

        String selectedPlans = item.getSelections().stream()
                .sorted(Comparator.comparing(selection -> selection.getPlanType().getCode()))
                .map(this::formatSelection)
                .toList()
                .toString();

        return productName + " - " + selectedPlans;
    }

    private String formatSelection(OrderItemSelection selection) {
        return selection.getPlanType().getName();
    }
}