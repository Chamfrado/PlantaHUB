package com.plantahub.api.storage;

import com.plantahub.api.service.admin.StorageDiagnosticsService;
import com.plantahub.api.shared.storage.BucketInspectionPort;
import com.plantahub.api.web.controller.admin.AdminStorageController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "O painel não reconfigura o bucket" precisa ser uma propriedade do código, não uma
 * promessa num comentário.
 *
 * <p>Aplicar CORS, lifecycle ou política pelo painel exigiria dar à aplicação
 * {@code s3:PutBucketCors}, {@code s3:PutLifecycleConfiguration} e
 * {@code s3:PutBucketPolicy} em produção — a capacidade de reescrever a própria segurança
 * do bucket. O ganho seria colar um JSON uma vez. O custo seria permanente: "o acervo
 * vendido vazou" passaria a ser um desfecho possível de um bug da aplicação, em vez de
 * exigir uma ação deliberada no console da AWS.
 *
 * <p>Se alguém precisar mudar isso um dia, que quebre aqui primeiro — é o lugar certo para
 * a conversa acontecer.
 */
class BucketInspectionIsReadOnlyTest {

    private static final List<String> FORBIDDEN =
            List.of("put", "set", "apply", "write", "delete", "create", "update", "configure");

    @Test
    @DisplayName("a porta de inspecao nao expoe nenhum metodo de escrita")
    void portHasNoWriteMethods() {
        List<String> offenders = Arrays.stream(BucketInspectionPort.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> FORBIDDEN.stream()
                        .anyMatch(bad -> name.toLowerCase(Locale.ROOT).startsWith(bad)))
                .toList();

        assertThat(offenders)
                .as("um metodo de escrita aqui significa pedir s3:PutBucket* em producao")
                .isEmpty();
    }

    @Test
    @DisplayName("o controller de armazenamento so tem leitura")
    void controllerExposesOnlyReads() {
        List<String> mutating = Arrays.stream(AdminStorageController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class)
                        || method.isAnnotationPresent(PutMapping.class)
                        || method.isAnnotationPresent(PatchMapping.class)
                        || method.isAnnotationPresent(DeleteMapping.class))
                .map(Method::getName)
                .toList();

        assertThat(mutating)
                .as("a tela diagnostica e entrega o JSON; quem aplica e uma pessoa, na AWS")
                .isEmpty();
    }

    @Test
    @DisplayName("o diagnostico nao recebe nenhuma dependencia capaz de reconfigurar o bucket")
    void serviceCannotReconfigureTheBucket() {
        var constructors = StorageDiagnosticsService.class.getConstructors();
        assertThat(constructors).hasSize(1);

        List<String> params = Arrays.stream(constructors[0].getParameterTypes())
                .map(Class::getSimpleName)
                .toList();

        assertThat(params)
                .as("um S3Client cru traria de volta, por acidente, tudo que esta interface evita")
                .doesNotContain("S3Client", "S3Presigner");
    }
}
