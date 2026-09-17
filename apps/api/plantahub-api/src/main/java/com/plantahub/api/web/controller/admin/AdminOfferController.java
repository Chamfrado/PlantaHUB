package com.plantahub.api.web.controller.admin;

import com.plantahub.api.service.admin.AdminOfferService;
import com.plantahub.api.web.dto.admin.AdminOfferDTOs.OfferDTO;
import com.plantahub.api.web.dto.admin.AdminOfferDTOs.OfferRequest;
import com.plantahub.api.web.dto.admin.AdminOfferDTOs.ReorderRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/products/{productId}/offers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOfferController {

    private final AdminOfferService service;

    public AdminOfferController(AdminOfferService service) {
        this.service = service;
    }

    @GetMapping
    public List<OfferDTO> list(@PathVariable String productId) {
        return service.list(productId);
    }

    /** Cria ou atualiza a oferta daquela colecao no produto. */
    @PutMapping("/{collectionCode}")
    public OfferDTO upsert(@PathVariable String productId,
                           @PathVariable String collectionCode,
                           @Valid @RequestBody OfferRequest request) {
        return service.upsert(productId, collectionCode, request);
    }

    @DeleteMapping("/{collectionCode}")
    public ResponseEntity<Void> remove(@PathVariable String productId,
                                       @PathVariable String collectionCode) {
        service.remove(productId, collectionCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reorder")
    public List<OfferDTO> reorder(@PathVariable String productId,
                                  @RequestBody ReorderRequest request) {
        service.reorder(productId, request.collectionCodes());
        return service.list(productId);
    }
}
