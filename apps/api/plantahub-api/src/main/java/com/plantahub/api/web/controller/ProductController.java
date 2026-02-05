package com.plantahub.api.web.controller;

import com.plantahub.api.service.ProductCatalogService;
import com.plantahub.api.web.dto.catalog.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/products")
public class ProductController {

    private final ProductCatalogService catalogService;

    public ProductController(ProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ProductSummaryDTO> list() {
        return catalogService.listProducts();
    }

    @GetMapping("/{category}/{slug}")
    public ProductDetailDTO detail(@PathVariable String category, @PathVariable String slug) {
        return catalogService.getProduct(category, slug);
    }

    @GetMapping("/{category}/{slug}/plan-types")
    public List<PlanTypeOptionDTO> planTypes(@PathVariable String category, @PathVariable String slug) {
        return catalogService.getPlanTypes(category, slug);
    }
}
