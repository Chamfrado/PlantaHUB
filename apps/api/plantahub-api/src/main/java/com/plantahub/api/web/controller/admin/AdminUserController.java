package com.plantahub.api.web.controller.admin;

import com.plantahub.api.domain.auth.enums.UserRole;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.shared.exception.NotFoundException;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Concessao e revogacao de acesso administrativo. */
@RestController
@RequestMapping("/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AppUserRepository userRepo;

    public AdminUserController(AppUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public record UserRoleRequest(@NotNull UserRole role) {}

    public record AdminUserDTO(UUID id, String email, String fullName, String role, boolean active) {}

    @GetMapping
    public List<AdminUserDTO> listAdmins() {
        return userRepo.findByRole(UserRole.ADMIN).stream()
                .map(u -> new AdminUserDTO(u.getId(), u.getEmail(), u.getFullName(),
                        u.getRole().name(), u.isActive()))
                .toList();
    }

    /**
     * Troca o papel de um usuario.
     *
     * <p>Duas travas contra travamento total: ninguem rebaixa a si mesmo (seria um tiro no
     * pe silencioso) e o ultimo administrador nao pode ser rebaixado — caso contrario o
     * painel ficaria inacessivel e so uma alteracao manual no banco resolveria.
     */
    @PatchMapping("/{userId}/role")
    @Transactional
    public AdminUserDTO changeRole(@AuthenticationPrincipal UserDetails caller,
                                   @PathVariable UUID userId,
                                   @RequestBody UserRoleRequest request) {
        var target = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("user_not_found"));

        boolean demoting = target.getRole() == UserRole.ADMIN && request.role() != UserRole.ADMIN;

        if (demoting) {
            if (target.getEmail().equalsIgnoreCase(caller.getUsername())) {
                throw new ConflictException("cannot_demote_yourself");
            }

            if (userRepo.countByRole(UserRole.ADMIN) <= 1) {
                throw new ConflictException("cannot_demote_last_admin");
            }
        }

        target.setRole(request.role());
        userRepo.save(target);

        return new AdminUserDTO(target.getId(), target.getEmail(), target.getFullName(),
                target.getRole().name(), target.isActive());
    }
}
