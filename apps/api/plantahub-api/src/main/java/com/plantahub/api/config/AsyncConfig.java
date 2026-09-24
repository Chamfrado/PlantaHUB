package com.plantahub.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor das tarefas administrativas longas (reconciliacao do bucket).
     *
     * <p>Deliberadamente minusculo e com fila curta: varrer o bucket inteiro e uma
     * operacao de manutencao disparada por uma pessoa, nao trafego. Um unico worker evita
     * que duas varreduras concorrentes disputem as mesmas linhas, e a fila curta faz uma
     * terceira falhar rapido em vez de acumular em silencio.
     */
    @Bean("adminTaskExecutor")
    public Executor adminTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("admin-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
