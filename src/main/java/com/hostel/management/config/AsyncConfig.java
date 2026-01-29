package com.hostel.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling  // ✅ Pour les tâches planifiées (nettoyage des codes expirés)
public class AsyncConfig {

    /**
     * ✅ Configuration de l'executor pour les tâches asynchrones
     * Utilisé pour l'envoi d'emails notamment
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // ✅ Nombre de threads minimum
        executor.setCorePoolSize(5);

        // ✅ Nombre de threads maximum
        executor.setMaxPoolSize(10);

        // ✅ Taille de la queue
        executor.setQueueCapacity(100);

        // ✅ Préfixe des noms de threads
        executor.setThreadNamePrefix("async-");

        // ✅ Comportement lors de l'arrêt
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}
