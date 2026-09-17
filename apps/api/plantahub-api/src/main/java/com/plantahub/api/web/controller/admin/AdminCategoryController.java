package com.plantahub.api.web.controller.admin;

import com.plantahub.api.service.admin.AdminCategoryService;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.CategoryDTO;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.CreateCategoryRequest;
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
