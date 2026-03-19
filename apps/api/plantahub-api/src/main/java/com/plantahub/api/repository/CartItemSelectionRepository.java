package com.plantahub.api.repository;

import com.plantahub.api.domain.cart.CartItemSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CartItemSelectionRepository extends JpaRepository<CartItemSelection, UUID> {

    @Query("""
        select s
        from CartItemSelection s
        join fetch s.planType pt
        where s.cartItem.id in :cartItemIds
    """)
    List<CartItemSelection> findAllByCartItemIdInWithPlanType(Collection<UUID> cartItemIds);
}