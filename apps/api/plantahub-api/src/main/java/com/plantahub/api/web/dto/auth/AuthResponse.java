package com.plantahub.api.web.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        String fullName,
        String email,
        /**
         * "USER" ou "ADMIN". Devolvido tanto no /login quanto no /me: se so viesse no
         * /me, um admin recem-logado ficaria sem papel ate dar refresh na pagina.
         *
         * <p>Serve apenas para a UI decidir o que mostrar. A autorizacao de verdade e
         * server-side, em cada endpoint /v1/admin/**.
         */
        String role
) {}
