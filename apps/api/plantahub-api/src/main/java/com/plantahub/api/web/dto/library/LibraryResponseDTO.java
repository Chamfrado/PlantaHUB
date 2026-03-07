package com.plantahub.api.web.dto.library;

import java.util.List;

public record LibraryResponseDTO(
        List<LibraryProductDTO> products
) {}
