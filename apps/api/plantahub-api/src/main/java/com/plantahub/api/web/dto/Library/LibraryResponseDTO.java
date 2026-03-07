package com.plantahub.api.web.dto.Library;

import java.util.List;

public record LibraryResponseDTO(
        List<LibraryProductDTO> products
) {}
