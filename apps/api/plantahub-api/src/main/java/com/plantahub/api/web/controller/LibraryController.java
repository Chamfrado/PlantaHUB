package com.plantahub.api.web.controller;

import com.plantahub.api.service.LibraryService;
import com.plantahub.api.web.dto.library.LibraryProductDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/me/library")
    public List<LibraryProductDTO> myLibrary(@AuthenticationPrincipal UserDetails user) {
        return libraryService.myLibrary(user.getUsername());
    }
}