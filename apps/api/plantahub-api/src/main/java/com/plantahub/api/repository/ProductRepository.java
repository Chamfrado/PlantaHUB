package com.plantahub.api.repository;

import com.plantahub.api.domain.catalog.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByActiveTrueOrderByCategoryAscNameAsc();

    Optional<Product> findByCategoryAndSlugAndActiveTrue(String category, String slug);
}
