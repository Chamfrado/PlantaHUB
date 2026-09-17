package com.plantahub.api.web.controller.admin;

import com.plantahub.api.service.admin.AdminAssetService;
import com.plantahub.api.web.dto.admin.AdminAssetDTOs.AssetDTO;
import com.plantahub.api.web.dto.admin.AdminAssetDTOs.MoveAssetRequest;
import com.plantahub.api.web.dto.admin.AdminAssetDTOs.ReorderRequest;
import com.plantahub.api.web.dto.admin.AdminAssetDTOs.UpdateAssetRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAssetController {

    private final AdminAssetService service;

    public AdminAssetController(AdminAssetService service) {
        this.service = service;
    }

    @GetMapping("/products/{productId}/assets")
    public List<AssetDTO> list(@PathVariable String productId,
                               @RequestParam(required = false) String collectionCode,
                               @RequestParam(defaultValue = "false") boolean includeDeleted) {
        return service.list(productId, collectionCode, includeDeleted).stream()
                .map(AssetDTO::from)
                .toList();
    }

    @PatchMapping("/assets/{assetId}")
    public AssetDTO update(@PathVariable UUID assetId,
                           @Valid @RequestBody UpdateAssetRequest request) {
        return AssetDTO.from(service.update(assetId, request));
    }

    /** Associar e desassociar: muda o vinculo, nunca a chave no bucket. */
    @PostMapping("/assets/{assetId}/move")
    public AssetDTO move(@PathVariable UUID assetId,
                         @Valid @RequestBody MoveAssetRequest request) {
        return AssetDTO.from(service.move(assetId, request.collectionCode()));
    }

    @PostMapping("/products/{productId}/assets/reorder")
    public List<AssetDTO> reorder(@PathVariable String productId,
                                  @RequestBody ReorderRequest request) {
        service.reorder(productId, request.assetIds());
        return service.list(productId, null, false).stream().map(AssetDTO::from).toList();
    }

    @DeleteMapping("/assets/{assetId}")
    public ResponseEntity<Void> delete(@PathVariable UUID assetId,
                                       @RequestParam(defaultValue = "false") boolean force) {
        service.delete(assetId, force);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assets/{assetId}/restore")
    public AssetDTO restore(@PathVariable UUID assetId) {
        return AssetDTO.from(service.restore(assetId));
    }
}
