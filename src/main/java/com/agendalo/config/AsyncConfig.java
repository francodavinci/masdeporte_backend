package com.agendalo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuración para procesamiento asíncrono de emails
 * Permite que el envío de emails no bloquee el flujo principal de la aplicación
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * Executor dedicado para el envío de emails
     * Configurado con un pool de hilos separado para evitar bloqueos
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configuración del pool de hilos
        executor.setCorePoolSize(2);           // Hilos mínimos siempre activos
        executor.setMaxPoolSize(5);            // Máximo de hilos en el pool
        executor.setQueueCapacity(100);        // Cola de tareas pendientes
        executor.setKeepAliveSeconds(60);      // Tiempo de vida de hilos inactivos
        executor.setThreadNamePrefix("Email-"); // Prefijo para identificar hilos de email
        
        // Política de rechazo cuando la cola está llena
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Configuración para shutdown graceful
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("Configurado executor asíncrono para emails: core={}, max={}, queue={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}
