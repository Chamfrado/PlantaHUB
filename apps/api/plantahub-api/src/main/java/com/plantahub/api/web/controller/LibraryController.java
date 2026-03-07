package com.plantahub.api.web.controller;

import com.plantahub.api.service.LibraryService;
import com.plantahub.api.web.dto.Library.LibraryProductDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/me/library")
    public List<LibraryProductDTO> myLibrary(@AuthenticationPrincipal Object principal) {
        String email = extractEmail(principal);
        return libraryService.myLibrary(email);
    }

    private String extractEmail(Object principal) {
        if (principal == null) return "";
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }
        return principal.toString();
    }
}