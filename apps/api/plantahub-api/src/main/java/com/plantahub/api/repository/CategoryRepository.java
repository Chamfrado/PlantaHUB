package com.plantahub.api.repository;

import com.plantahub.api.domain.catalog.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, String> {

    List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<Category> findAllByOrderBySortOrderAscNameAsc();
}
