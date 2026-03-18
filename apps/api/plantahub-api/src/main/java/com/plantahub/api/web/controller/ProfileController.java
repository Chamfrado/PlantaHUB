package com.plantahub.api.web.controller;

import com.plantahub.api.service.ProfileService;
import com.plantahub.api.web.dto.profile.DeleteProfileRequest;
import com.plantahub.api.web.dto.profile.DeleteProfileResponse;
import com.plantahub.api.web.dto.profile.ProfileResponse;
import com.plantahub.api.web.dto.profile.ProfileStatusResponse;
import com.plantahub.api.web.dto.profile.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile(@AuthenticationPrincipal UserDetails user) {
        return profileService.getProfile(user.getUsername());
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody UpdateProfileRequest req
    ) {
        return profileService.updateProfile(user.getUsername(), req);
    }

    @GetMapping("/profile/status")
    public ProfileStatusResponse getProfileStatus(@AuthenticationPrincipal UserDetails user) {
        return profileService.getProfileStatus(user.getUsername());
    }

    @PostMapping("/profile/delete")
    public DeleteProfileResponse deleteProfile(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody DeleteProfileRequest req
    ) {
        return profileService.deleteProfile(user.getUsername(), req.confirmationText());
    }
}