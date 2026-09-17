package com.plantahub.api.web.controller.admin;

import com.plantahub.api.service.admin.AdminCollectionService;
import com.plantahub.api.web.dto.admin.AdminCollectionDTOs.CollectionDTO;
import com.plantahub.api.web.dto.admin.AdminCollectionDTOs.CreateCollectionRequest;
import com.plantahub.api.web.dto.admin.AdminCollectionDTOs.UpdateCollectionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/collections")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCollectionController {

    private final AdminCollectionService service;

    public AdminCollectionController(AdminCollectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<CollectionDTO> list() {
        return service.list().stream()
                .map(c -> CollectionDTO.from(c, service.assetCount(c.getId())))
                .toList();
    }

    @PostMapping
    public ResponseEntity<CollectionDTO> create(@Valid @RequestBody CreateCollectionRequest request) {
        var created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CollectionDTO.from(created, 0));
    }

    @GetMapping("/{id}")
    public CollectionDTO get(@PathVariable UUID id) {
        return CollectionDTO.from(service.get(id), service.assetCount(id));
    }

    @PutMapping("/{id}")
    public CollectionDTO update(@PathVariable UUID id,
                                @Valid @RequestBody UpdateCollectionRequest request) {
        return CollectionDTO.from(service.update(id, request), service.assetCount(id));
    }

    @PostMapping("/{id}/activate")
    public CollectionDTO activate(@PathVariable UUID id) {
        return CollectionDTO.from(service.setActive(id, true), service.assetCount(id));
    }

    @PostMapping("/{id}/deactivate")
    public CollectionDTO deactivate(@PathVariable UUID id) {
        return CollectionDTO.from(service.setActive(id, false), service.assetCount(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
