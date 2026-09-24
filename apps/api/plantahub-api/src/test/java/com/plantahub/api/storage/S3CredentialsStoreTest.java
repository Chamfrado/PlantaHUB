package com.plantahub.api.storage;

import com.plantahub.api.domain.config.AppSecret;
import com.plantahub.api.repository.AppSecretRepository;
import com.plantahub.api.service.admin.S3CredentialsStore;
import com.plantahub.api.service.admin.S3CredentialsStore.Source;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.support.AbstractApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As credenciais que o painel grava.
 *
 * <p>Duas propriedades importam mais que a funcionalidade: o segredo <b>não</b> fica legível
 * no banco, e o segredo <b>não</b> volta por nenhum caminho de leitura. A primeira limita o
 * estrago de um dump; a segunda impede que um XSS no painel vire uma chave da AWS na mão de
 * outra pessoa.
 *
 * <p>Sem {@code @Transactional}: o cache interno é invalidado na escrita, e um rollback
 * automático esconderia se a invalidação de fato aconteceu.
 */
class S3CredentialsStoreTest extends AbstractApiTest {

    @Autowired private S3CredentialsStore store;
    @Autowired private AppSecretRepository secretRepo;

    private void reset() {
        store.clear("teste");
    }

    @Test
    @DisplayName("o segredo nao fica legivel no banco")
    void secretIsEncryptedAtRest() {
        try {
            store.save("AKIAEXEMPLO12345", "segredo-muito-secreto", "admin@plantahub.test");

            var row = secretRepo.findById(AppSecret.S3_CREDENTIALS).orElseThrow();

            assertThat(row.getValueEncrypted())
                    .as("um dump do banco nao pode entregar a chave da AWS junto")
                    .doesNotContain("segredo-muito-secreto")
                    .doesNotContain("AKIAEXEMPLO12345");
        } finally {
            reset();
        }
    }

    @Test
    @DisplayName("o status identifica a chave sem revelar nada")
    void statusIdentifiesWithoutRevealing() {
        try {
            var status = store.save("AKIAEXEMPLO12345", "segredo", "admin@plantahub.test");

            assertThat(status.source()).isEqualTo(Source.PANEL);
            assertThat(status.updatedBy()).isEqualTo("admin@plantahub.test");

            assertThat(status.accessKeyHint())
                    .as("o suficiente para saber QUAL chave esta em uso, e so")
                    .isEqualTo("••••2345");

            assertThat(status.toString())
                    .as("nem por acidente, num log ou numa serializacao")
                    .doesNotContain("segredo");
        } finally {
            reset();
        }
    }

    @Test
    @DisplayName("o que foi gravado volta utilizavel para assinar")
    void roundTripsForSigning() {
        try {
            store.save("AKIAEXEMPLO12345", "segredo-muito-secreto", "admin@plantahub.test");

            var credentials = store.panelCredentials().orElseThrow();

            assertThat(credentials.accessKey()).isEqualTo("AKIAEXEMPLO12345");
            assertThat(credentials.secretKey()).isEqualTo("segredo-muito-secreto");
        } finally {
            reset();
        }
    }

    @Test
    @DisplayName("trocar a credencial vale na proxima assinatura, sem reiniciar")
    void replacingTakesEffectImmediately() {
        try {
            store.save("AKIAPRIMEIRA0000", "primeiro", "admin@plantahub.test");
            assertThat(store.panelCredentials().orElseThrow().accessKey()).isEqualTo("AKIAPRIMEIRA0000");

            store.save("AKIASEGUNDA00000", "segundo", "admin@plantahub.test");

            assertThat(store.panelCredentials().orElseThrow().accessKey())
                    .as("o cache existe para o SDK nao consultar o banco a cada assinatura; "
                            + "se ele nao invalidar na escrita, a troca so valeria apos restart")
                    .isEqualTo("AKIASEGUNDA00000");
        } finally {
            reset();
        }
    }

    @Test
    @DisplayName("remover volta para a fonte anterior")
    void clearingFallsBack() {
        store.save("AKIAEXEMPLO12345", "segredo", "admin@plantahub.test");
        assertThat(store.status().source()).isEqualTo(Source.PANEL);

        var afterClear = store.clear("admin@plantahub.test");

        assertThat(afterClear.source())
                .as("no ambiente de teste nao ha chave configurada, entao sobra a cadeia padrao")
                .isEqualTo(Source.DEFAULT_CHAIN);
        assertThat(store.panelCredentials()).isEmpty();
    }

    @Test
    @DisplayName("credencial pela metade e recusada em vez de gravada quebrada")
    void incompleteCredentialsAreRejected() {
        assertThatThrownBy(() -> store.save("AKIAEXEMPLO12345", "  ", "admin@plantahub.test"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("credentials_incomplete");

        assertThat(secretRepo.findById(AppSecret.S3_CREDENTIALS))
                .as("uma gravacao parcial deixaria a aplicacao sem assinar e sem explicacao")
                .isEmpty();
    }
}
