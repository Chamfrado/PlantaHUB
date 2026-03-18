package com.plantahub.api.web.controller;

import com.plantahub.api.service.ProfileService;
import com.plantahub.api.web.dto.profile.ProfileResponse;
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
            System.out.println(profileService.getProfile(user.getUsername()).role());
            if( profileService.getProfile(user.getUsername()).role() != null){
                return  profileService.getProfile(user.getUsername());
            }else{
                throw new IllegalArgumentException("user is not logged in");
            }

        }

        @PutMapping("/profile")
        public ProfileResponse updateProfile(
                @AuthenticationPrincipal UserDetails user,
                @Valid @RequestBody UpdateProfileRequest req
        ) {
            return profileService.updateProfile(user.getUsername(), req);
        }
    }
