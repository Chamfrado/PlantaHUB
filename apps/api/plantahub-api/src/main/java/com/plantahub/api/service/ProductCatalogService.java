package com.plantahub.api.service;

import com.plantahub.api.repository.ProductPlanTypeRepository;
import com.plantahub.api.repository.ProductRepository;
import com.plantahub.api.web.dto.catalog.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductCatalogService {

    private final ProductRepository productRepository;
    private final ProductPlanTypeRepository productPlanTypeRepository;

    public ProductCatalogService(ProductRepository productRepository,
                                 ProductPlanTypeRepository productPlanTypeRepository) {
        this.productRepository = productRepository;
        this.productPlanTypeRepository = productPlanTypeRepository;
    }

    public List<ProductSummaryDTO> listProducts(String category, Integer limit) {
        List<ProductSummaryDTO> items;

        if (category != null && !category.isBlank()) {
            items = productRepository.findByCategoryAndActiveTrueOrderByNameAsc(category)
                    .stream()
                    .map(p -> new ProductSummaryDTO(
                            p.getId(),
                            p.getCategory(),
                            p.getSlug(),
                            p.getName(),
                            p.getShortDesc(),
                            p.getAreaM2(),
                            p.getHeroImageUrl(),
                            p.getCustomizable(),
                            p.getBasePriceCents()
                    ))
                    .toList();
        } else {
            items = productRepository.findByActiveTrueOrderByCategoryAscNameAsc()
                    .stream()
                    .map(p -> new ProductSummaryDTO(
                            p.getId(),
                            p.getCategory(),
                            p.getSlug(),
                            p.getName(),
                            p.getShortDesc(),
                            p.getAreaM2(),
                            p.getHeroImageUrl(),
                            p.getCustomizable(),
                            p.getBasePriceCents()
                    ))
                    .toList();
        }

        if (limit != null && limit > 0 && items.size() > limit) {
            return items.subList(0, limit);
        }

        return items;
    }

    public ProductDetailDTO getProduct(String category, String slug) {
        var p = productRepository.findByCategoryAndSlugAndActiveTrue(category, slug)
                .orElseThrow(() -> new IllegalArgumentException("product_not_found"));

        return new ProductDetailDTO(
                p.getId(),
                p.getCategory(),
                p.getSlug(),
                p.getName(),
                p.getShortDesc(),
                p.getAreaM2(),
                p.getHeroImageUrl(),
                p.getDelivery(),
                p.getCustomizable()
        );
    }

    public List<PlanTypeOptionDTO> getPlanTypes(String category, String slug) {
        var p = productRepository.findByCategoryAndSlugAndActiveTrue(category, slug)
                .orElseThrow(() -> new IllegalArgumentException("product_not_found"));

        return productPlanTypeRepository.findAvailableByProductIdWithPlanType(p.getId())
                .stream()
                .map(ppt -> new PlanTypeOptionDTO(
                        ppt.getPlanType().getCode(),
                        ppt.getPlanType().getName(),
                        ppt.getPlanType().getDescription(),
                        ppt.getPriceCents(),
                        ppt.getIncludedInBundle()
                ))
                .toList();
    }
}