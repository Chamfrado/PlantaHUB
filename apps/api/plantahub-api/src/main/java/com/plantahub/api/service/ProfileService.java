package com.plantahub.api.service;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.shared.util.CpfUtils;
import com.plantahub.api.shared.util.PhoneUtils;
import com.plantahub.api.web.dto.profile.ProfileResponse;
import com.plantahub.api.web.dto.profile.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final AppUserRepository repo;

    public ProfileService(AppUserRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest req) {
        var user = repo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        if (req.fullName() != null && !req.fullName().isBlank()) {
            user.setFullName(req.fullName().trim());
        }

        if (req.cpf() != null && !req.cpf().isBlank()) {
            String normalizedCpf = req.cpf().replaceAll("[^0-9]", "");

            if (!CpfUtils.isValid(normalizedCpf)) {
                throw new IllegalArgumentException("cpf_invalid");
            }

            if (user.getCpf() != null && !user.getCpf().isBlank()) {
                if (!user.getCpf().equals(normalizedCpf)) {
                    throw new IllegalArgumentException("cpf_already_confirmed");
                }
            } else {
                repo.findByCpf(normalizedCpf)
                        .filter(existing -> !existing.getId().equals(user.getId()))
                        .ifPresent(existing -> {
                            throw new IllegalArgumentException("cpf_already_in_use");
                        });

                user.setCpf(normalizedCpf);
            }
        }

        if (req.phoneNumber() != null && !req.phoneNumber().isBlank()) {
            if (!PhoneUtils.isValid(req.phoneNumber())) {
                throw new IllegalArgumentException("phone_invalid");
            }

            user.setPhoneNumber(PhoneUtils.normalize(req.phoneNumber()));
        }

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        var user = repo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        return toResponse(user);
    }

    private ProfileResponse toResponse(AppUser user) {
        boolean cpfLocked = user.getCpf() != null && !user.getCpf().isBlank();

        boolean profileCompleted =
                user.getFullName() != null && !user.getFullName().isBlank()
                        && user.getCpf() != null && !user.getCpf().isBlank()
                        && user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank();

        return new ProfileResponse(
                user.getEmail(),
                user.getFullName(),
                CpfUtils.mask(user.getCpf()),
                PhoneUtils.mask(user.getPhoneNumber()),
                user.getRole().name(),
                cpfLocked,
                profileCompleted
        );
    }
}