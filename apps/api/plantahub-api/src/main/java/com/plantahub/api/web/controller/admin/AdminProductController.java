package com.plantahub.api.web.controller.admin;

import com.plantahub.api.domain.catalog.content.ProductContent;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.service.ProductCatalogService;
import com.plantahub.api.service.admin.AdminProductService;
import com.plantahub.api.web.dto.admin.AdminProductDTOs.AdminProductSummaryDTO;
import com.plantahub.api.web.dto.admin.AdminProductDTOs.CreateProductRequest;
import com.plantahub.api.web.dto.admin.AdminProductDTOs.UpdateProductRequest;
import com.plantahub.api.web.dto.catalog.PlanTypeOptionDTO;
import com.plantahub.api.web.dto.catalog.ProductDetailDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService productService;
    private final ProductCatalogService catalogService;

    public AdminProductController(AdminProductService productService,
                                  ProductCatalogService catalogService) {
        this.productService = productService;
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<AdminProductSummaryDTO> list(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q
    ) {
        return productService.listSummaries(status, category, q);
    }

    @PostMapping
    public ResponseEntity<ProductDetailDTO> create(@Valid @RequestBody CreateProductRequest request) {
        var product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.assembleDetail(product));
    }

    @GetMapping("/{id}")
    public ProductDetailDTO get(@PathVariable String id) {
        return catalogService.assembleDetail(productService.get(id));
    }

    @PutMapping("/{id}")
    public ProductDetailDTO update(@PathVariable String id,
                                   @Valid @RequestBody UpdateProductRequest request) {
        return catalogService.assembleDetail(productService.update(id, request));
    }

    /** Substitui o documento de conteudo inteiro. */
    @PutMapping("/{id}/content")
    public ProductDetailDTO replaceContent(@PathVariable String id,
                                           @Valid @RequestBody ProductContent content) {
        return catalogService.assembleDetail(productService.replaceContent(id, content));
    }

    @PostMapping("/{id}/publish")
    public ProductDetailDTO publish(@PathVariable String id) {
        return catalogService.assembleDetail(productService.publish(id));
    }

    @PostMapping("/{id}/unpublish")
    public ProductDetailDTO unpublish(@PathVariable String id) {
        return catalogService.assembleDetail(productService.unpublish(id));
    }

    @PostMapping("/{id}/archive")
    public ProductDetailDTO archive(@PathVariable String id) {
        return catalogService.assembleDetail(productService.archive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** O que ainda falta para publicar, sem tentar publicar. */
    @GetMapping("/{id}/publish-check")
    public Map<String, Object> publishCheck(@PathVariable String id) {
        var problems = productService.publishProblems(productService.get(id));
        return Map.of("publishable", problems.isEmpty(), "problems", problems);
    }

    /**
     * Pre-visualizacao.
     *
     * <p>Mesmos DTOs e mesmo assembler da rota publica: a unica diferenca e que aqui nao ha
     * filtro de status. Um DTO separado permitiria que preview e pagina publica
     * divergissem, que e exatamente o que uma pre-visualizacao nao pode fazer.
     */
    @GetMapping("/{id}/preview")
    public Map<String, Object> preview(@PathVariable String id) {
        var product = productService.get(id);

        return Map.of(
                "product", catalogService.assembleDetail(product),
                "planTypes", assemblePlanTypes(product)
        );
    }

    private List<PlanTypeOptionDTO> assemblePlanTypes(com.plantahub.api.domain.catalog.Product product) {
        return catalogService.assemblePlanTypes(product);
    }
}
