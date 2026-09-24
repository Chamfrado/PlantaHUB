package com.plantahub.api.web;

import com.plantahub.api.support.AbstractApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * O teste de autorizacao de maior alcance da suite.
 *
 * <p>As rotas nao sao uma lista escrita a mao: sao descobertas em runtime a partir do
 * mapeamento do Spring MVC. Isso significa que um endpoint administrativo novo passa a ser
 * coberto no dia em que e escrito, sem ninguem precisar lembrar de acrescentar um caso —
 * que e exatamente o erro mais provavel conforme a superficie admin cresce de duas rotas
 * para dezenas.
 *
 * <p>Sao verificados os dois casos que nunca chegam ao handler: anonimo e usuario comum.
 * Exercitar o caso ADMIN genericamente executaria de verdade cada endpoint (disparando
 * varreduras, por exemplo); o comportamento com ADMIN e coberto pelos testes proprios de
 * cada servico.
 */
@AutoConfigureMockMvc
class AdminAuthorizationTest extends AbstractApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RequestMappingHandlerMapping handlerMapping;

    private record Route(HttpMethod method, String pattern) {}

    private List<Route> adminRoutes() {
        Set<Route> routes = new LinkedHashSet<>();

        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            Set<String> patterns = info.getPathPatternsCondition() != null
                    ? info.getPathPatternsCondition().getPatternValues()
                    : Set.of();

            for (String pattern : patterns) {
                if (!pattern.startsWith("/v1/admin")) {
                    continue;
                }

                var methods = info.getMethodsCondition().getMethods();

                if (methods.isEmpty()) {
                    routes.add(new Route(HttpMethod.GET, pattern));
                } else {
                    methods.forEach(m -> routes.add(new Route(HttpMethod.valueOf(m.name()), pattern)));
                }
            }
        }

        return new ArrayList<>(routes);
    }

    /** Troca {@code {id}} por um valor concreto: o objetivo e autorizacao, nao roteamento. */
    private String concrete(String pattern) {
        return pattern.replaceAll("\\{[^/}]+}", "00000000-0000-0000-0000-000000000000");
    }

    private MockHttpServletRequestBuilder request(Route route) {
        return MockMvcRequestBuilders.request(route.method(), concrete(route.pattern()));
    }

    @TestFactory
    @DisplayName("toda rota /v1/admin exige autenticacao")
    Stream<DynamicTest> anonymousIsRejected() {
        List<Route> routes = adminRoutes();

        assertThat(routes)
                .as("nenhuma rota admin encontrada: o teste ficaria verde sem verificar nada")
                .isNotEmpty();

        return routes.stream().map(route -> DynamicTest.dynamicTest(
                "anonimo " + route.method() + " " + route.pattern(),
                () -> mockMvc.perform(request(route))
                        .andExpect(result -> assertThat(result.getResponse().getStatus())
                                .as("anonimo nunca pode ser atendido")
                                .isIn(401, 403))));
    }

    @TestFactory
    @DisplayName("um usuario comum recebe 403 em toda rota /v1/admin")
    Stream<DynamicTest> regularUserIsForbidden() {
        return adminRoutes().stream().map(route -> DynamicTest.dynamicTest(
                "USER " + route.method() + " " + route.pattern(),
                () -> mockMvc.perform(request(route).with(user("cliente@plantahub.test").roles("USER")))
                        .andExpect(result -> assertThat(result.getResponse().getStatus())
                                .as("cliente comum nao pode administrar o catalogo")
                                .isEqualTo(403))));
    }
}
