package com.needlos.common.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Map;

/**
 * Tareas en segundo plano (@Async) con el ejecutor que configura Spring Boot.
 * El decorador copia el MDC para que los logs conserven el id de correlacion.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    TaskDecorator copiarContextoDeLogs() {
        return tarea -> {
            Map<String, String> contexto = MDC.getCopyOfContextMap();
            return () -> {
                if (contexto != null) {
                    MDC.setContextMap(contexto);
                }
                try {
                    tarea.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }
}
