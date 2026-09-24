package com.plantahub.api.web.controller.admin;

import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.service.admin.AdminCategoryService;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.CategoryDTO;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.CategoryProductDTO;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.CreateCategoryRequest;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.ReorderRequest;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.UpdateCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final AdminCategoryService service;

    public AdminCategoryController(AdminCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryDTO> list() {
        return service.list().stream()
                .map(c -> CategoryDTO.from(c, service.productCount(c.getSlug())))
                .toList();
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> create(@Valid @RequestBody CreateCategoryRequest request) {
        var created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryDTO.from(created, 0));
    }

    @PutMapping("/order")
    public List<CategoryDTO> reorder(@Valid @RequestBody ReorderRequest request) {
        return service.reorder(request.ids()).stream()
                .map(c -> CategoryDTO.from(c, service.productCount(c.getSlug())))
                .toList();
    }

    @GetMapping("/{slug}/products")
    public List<CategoryProductDTO> products(@PathVariable String slug) {
        return service.products(slug).stream().map(AdminCategoryController::toProductDto).toList();
    }

    @PutMapping("/{slug}/products/order")
    public List<CategoryProductDTO> reorderProducts(@PathVariable String slug,
                                                    @Valid @RequestBody ReorderRequest request) {
        return service.reorderProducts(slug, request.ids()).stream()
                .map(AdminCategoryController::toProductDto)
                .toList();
    }

    private static CategoryProductDTO toProductDto(Product p) {
        return new CategoryProductDTO(p.getId(), p.getName(), p.getStatus().name());
    }

    @PutMapping("/{slug}")
    public CategoryDTO update(@PathVariable String slug,
                              @Valid @RequestBody UpdateCategoryRequest request) {
        return CategoryDTO.from(service.update(slug, request), service.productCount(slug));
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        service.delete(slug);
        return ResponseEntity.noContent().build();
    }
}
