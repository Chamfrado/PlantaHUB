package com.plantahub.api.web.controller.admin;

import com.plantahub.api.service.admin.AdminMediaService;
import com.plantahub.api.web.dto.admin.AdminMediaDTOs.MediaDTO;
import com.plantahub.api.web.dto.admin.AdminMediaDTOs.ReorderRequest;
import com.plantahub.api.web.dto.admin.AdminMediaDTOs.UpdateMediaRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMediaController {

    private final AdminMediaService service;

    public AdminMediaController(AdminMediaService service) {
        this.service = service;
    }

    @GetMapping("/products/{productId}/media")
    public List<MediaDTO> list(@PathVariable String productId) {
        return service.list(productId).stream().map(MediaDTO::from).toList();
    }

    @PatchMapping("/media/{mediaId}")
    public MediaDTO update(@PathVariable UUID mediaId,
                           @Valid @RequestBody UpdateMediaRequest request) {
        return MediaDTO.from(service.update(mediaId, request));
    }

    @PostMapping("/products/{productId}/media/reorder")
    public List<MediaDTO> reorder(@PathVariable String productId,
                                  @RequestBody ReorderRequest request) {
        service.reorder(productId, request.mediaIds());
        return service.list(productId).stream().map(MediaDTO::from).toList();
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<Void> delete(@PathVariable UUID mediaId) {
        service.delete(mediaId);
        return ResponseEntity.noContent().build();
    }
}
