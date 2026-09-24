package com.plantahub.api.reconciliation;

import com.plantahub.api.service.admin.ReconciliationService;
import com.plantahub.api.shared.storage.ObjectStorageReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "A reconciliacao nunca apaga nada" precisa ser uma propriedade do tipo, nao uma promessa
 * num comentario.
 *
 * <p>O servico depende de {@link ObjectStorageReader}, uma interface deliberadamente
 * estreita. Se alguem adicionar um metodo de escrita ou remocao ali, ou trocar a
 * dependencia pela porta completa, estes testes quebram — que e o momento certo de
 * perguntar por que.
 */
class ReconciliationNoDeleteTest {

    private static final List<String> FORBIDDEN = List.of("delete", "remove", "put", "write", "upload");

    @Test
    @DisplayName("ObjectStorageReader nao expoe nenhum metodo destrutivo")
    void readerHasNoDestructiveMethods() {
        List<String> offenders = Arrays.stream(ObjectStorageReader.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> FORBIDDEN.stream()
                        .anyMatch(bad -> name.toLowerCase(Locale.ROOT).contains(bad)))
                .toList();

        assertThat(offenders)
                .as("ObjectStorageReader e a garantia estrutural de que a reconciliacao "
                        + "nao pode destruir dados; metodos de escrita pertencem a ObjectStoragePort")
                .isEmpty();
    }

    @Test
    @DisplayName("o servico recebe o leitor estreito, nao a porta completa")
    void serviceDependsOnTheNarrowInterface() {
        Constructor<?>[] constructors = ReconciliationService.class.getConstructors();

        assertThat(constructors).hasSize(1);

        List<Class<?>> params = Arrays.asList(constructors[0].getParameterTypes());

        assertThat(params)
                .as("trocar por ObjectStoragePort devolveria ao servico a capacidade de escrever")
                .contains(ObjectStorageReader.class);

        assertThat(params)
                .noneMatch(type -> type.getSimpleName().equals("ObjectStoragePort"));
    }
}
