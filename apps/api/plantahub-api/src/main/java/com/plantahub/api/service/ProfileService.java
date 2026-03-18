package com.plantahub.api.service;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.shared.exception.ProfileIncompleteException;
import com.plantahub.api.shared.util.CpfUtils;
import com.plantahub.api.shared.util.PhoneUtils;
import com.plantahub.api.web.dto.profile.DeleteProfileResponse;
import com.plantahub.api.web.dto.profile.ProfileResponse;
import com.plantahub.api.web.dto.profile.ProfileStatusResponse;
import com.plantahub.api.web.dto.profile.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ProfileService {

    private static final String DELETE_CONFIRMATION_TEXT = "eu quero excluir minha conta";

    private final AppUserRepository repo;

    public ProfileService(AppUserRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest req) {
        var user = findActiveUserByEmail(email);

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
        var user = findActiveUserByEmail(email);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<String> getMissingProfileFields(String email) {
        var user = findActiveUserByEmail(email);

        List<String> missing = new ArrayList<>();

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            missing.add("fullName");
        }
        if (user.getCpf() == null || user.getCpf().isBlank()) {
            missing.add("cpf");
        }
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            missing.add("phoneNumber");
        }

        return missing;
    }

    @Transactional(readOnly = true)
    public void assertProfileComplete(String email) {
        var missing = getMissingProfileFields(email);

        if (!missing.isEmpty()) {
            throw new ProfileIncompleteException(missing);
        }
    }

    @Transactional(readOnly = true)
    public ProfileStatusResponse getProfileStatus(String email) {
        var user = findActiveUserByEmail(email);

        var missingFields = getMissingProfileFields(email);

        boolean cpfLocked = user.getCpf() != null && !user.getCpf().isBlank();
        boolean profileCompleted = missingFields.isEmpty();

        return new ProfileStatusResponse(
                profileCompleted,
                cpfLocked,
                missingFields
        );
    }

    @Transactional
    public DeleteProfileResponse deleteProfile(String email, String confirmationText) {
        var user = findActiveUserByEmail(email);

        String normalizedText = normalizeConfirmationText(confirmationText);
        if (!DELETE_CONFIRMATION_TEXT.equals(normalizedText)) {
            throw new IllegalArgumentException("delete_confirmation_invalid");
        }

        user.setActive(false);
        user.setDeletedAt(Instant.now());

        return new DeleteProfileResponse(true, user.getDeletedAt());
    }

    private AppUser findActiveUserByEmail(String email) {
        return repo.findByEmailAndActiveTrueAndDeletedAtIsNull(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));
    }

    private String normalizeConfirmationText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
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