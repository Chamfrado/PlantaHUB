package com.plantahub.api.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Base para testes que sobem o contexto inteiro: Postgres real (via
 * {@link AbstractPostgresTest}) e armazenamento de objetos em memoria.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestStorageConfig.class, TestDataFactory.class})
public abstract class AbstractApiTest extends AbstractPostgresTest {
}
