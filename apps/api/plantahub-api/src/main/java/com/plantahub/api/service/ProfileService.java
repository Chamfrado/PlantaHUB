package com.plantahub.api.service;

import com.plantahub.api.repository.AppUserRepository;
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

        if (req.fullName() != null) {
            user.setFullName(req.fullName().trim());
        }

        if (req.cpf() != null) {
            String normalizedCpf = req.cpf().replaceAll("[^0-9]", "");
            user.setCpf(normalizedCpf);
        }

        if (req.phoneNumber() != null) {
            user.setPhoneNumber(req.phoneNumber().trim());
        }

        return new ProfileResponse(
                user.getEmail(),
                user.getFullName(),
                user.getCpf(),
                user.getPhoneNumber(),
                user.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        var user = repo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        return new ProfileResponse(
                user.getEmail(),
                user.getFullName(),
                user.getCpf(),
                user.getPhoneNumber(),
                user.getRole().name()
        );
    }
}
