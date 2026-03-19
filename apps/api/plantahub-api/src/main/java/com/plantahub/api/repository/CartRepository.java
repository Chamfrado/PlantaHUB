package com.plantahub.api.repository;

import com.plantahub.api.domain.cart.Cart;
import com.plantahub.api.domain.cart.enums.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUser_EmailAndStatus(String email, CartStatus status);

    @Query("""
        select distinct c
        from Cart c
        left join fetch c.items i
        left join fetch i.product p
        where c.user.email = :email
          and c.status = :status
    """)
    Optional<Cart> findDetailedByUserEmailAndStatus(String email, CartStatus status);
}