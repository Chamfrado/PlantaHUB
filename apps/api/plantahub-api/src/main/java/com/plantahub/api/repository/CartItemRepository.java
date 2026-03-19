package com.plantahub.api.repository;

import com.plantahub.api.domain.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByIdAndCart_User_Email(UUID id, String email);
}