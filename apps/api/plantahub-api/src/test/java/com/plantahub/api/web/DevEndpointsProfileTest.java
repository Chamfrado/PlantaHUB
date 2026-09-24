package com.plantahub.api.web;

import com.plantahub.api.web.controller.DevOrderController;
import com.plantahub.api.web.controller.EntitlementController;
import com.plantahub.api.web.controller.OrderController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os atalhos de pagamento de desenvolvimento nao podem existir em producao.
 *
 * <p>{@code pay-mock}, {@code refund-mock} e {@code mark-paid} marcam um pedido como pago
 * e liberam os arquivos sem cobrar nada. Enquanto viveram no {@code OrderController} e no
 * {@code EntitlementController}, qualquer cliente autenticado podia se dar produtos de
 * graca em producao.
 *
 * <p>Teste de reflexao de proposito: subir um contexto com o perfil {@code prod} exigiria
 * as variaveis de ambiente de producao. O que precisa ser garantido aqui e que a anotacao
 * esta no lugar e que as rotas nao voltaram para um controller sempre ativo.
 */
class DevEndpointsProfileTest {

    @Test
    @DisplayName("DevOrderController esta fora do perfil prod")
    void devControllerIsExcludedFromProd() {
        Profile profile = DevOrderController.class.getAnnotation(Profile.class);

        assertThat(profile)
                .as("DevOrderController precisa de @Profile para nao existir em producao")
                .isNotNull();

        assertThat(profile.value()).containsExactly("!prod");
    }

    @Test
    @DisplayName("os atalhos de pagamento nao voltaram para controllers sempre ativos")
    void mockEndpointsAreNotInAlwaysOnControllers() {
        assertThat(postMappings(OrderController.class))
                .noneMatch(path -> path.contains("pay-mock") || path.contains("refund-mock"));

        assertThat(postMappings(EntitlementController.class))
                .noneMatch(path -> path.contains("mark-paid"));
    }

    @Test
    @DisplayName("os tres atalhos estao no controller de dev")
    void devControllerHoldsAllShortcuts() {
        assertThat(postMappings(DevOrderController.class))
                .anyMatch(path -> path.contains("pay-mock"))
                .anyMatch(path -> path.contains("refund-mock"))
                .anyMatch(path -> path.contains("mark-paid"));
    }

    private java.util.List<String> postMappings(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .map(Method::getAnnotations)
                .flatMap(Arrays::stream)
                .filter(PostMapping.class::isInstance)
                .map(PostMapping.class::cast)
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .toList();
    }
}
