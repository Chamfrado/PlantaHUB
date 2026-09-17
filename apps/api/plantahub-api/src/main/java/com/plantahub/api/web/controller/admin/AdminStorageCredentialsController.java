package com.plantahub.api.web.controller.admin;

import com.plantahub.api.service.admin.S3CredentialsStore;
import com.plantahub.api.service.admin.S3CredentialsStore.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * As credenciais da S3, definidas pelo painel.
 *
 * <p>Separado de {@link AdminStorageController} de propósito: aquele é só leitura, e um
 * teste garante isso. A distinção que importa não é "escreve ou não", é <b>o que</b> se
 * escreve — credencial é configuração desta aplicação; CORS, lifecycle e política são
 * configuração do bucket, e essas o painel não toca.
 *
 * <p><b>O segredo nunca volta.</b> Nenhuma resposta daqui contém a secret key: só a fonte
 * em uso, os últimos quatro caracteres da chave pública e quando foi trocada. Um campo que
 * devolve o próprio segredo para preencher o formulário transforma qualquer XSS no painel
 * num vazamento de credencial da AWS.
 */
@RestController
@RequestMapping("/v1/admin/storage/credentials")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStorageCredentialsController {

    public record CredentialsRequest(
            @NotBlank String accessKey,
            @NotBlank String secretKey
    ) {}

    private final S3CredentialsStore store;

    public AdminStorageCredentialsController(S3CredentialsStore store) {
        this.store = store;
    }

    @GetMapping
    public Status status() {
        return store.status();
    }

    @PutMapping
    public Status save(@Valid @RequestBody CredentialsRequest request,
                       @AuthenticationPrincipal UserDetails user) {
        return store.save(request.accessKey(), request.secretKey(), usernameOf(user));
    }

    /** Volta para a configuração ou para a cadeia padrão do SDK. */
    @DeleteMapping
    public Status clear(@AuthenticationPrincipal UserDetails user) {
        return store.clear(usernameOf(user));
    }

    private static String usernameOf(UserDetails user) {
        return user == null ? "desconhecido" : user.getUsername();
    }
}
