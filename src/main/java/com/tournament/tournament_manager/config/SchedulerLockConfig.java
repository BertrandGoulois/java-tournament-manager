package com.tournament.tournament_manager.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration du verrou distribué ShedLock (voir {@code PurgeScheduler}).
 *
 * <p>Réutilise la base Postgres déjà en place (table {@code shedlock}, migration 017) —
 * pas de nouvelle infrastructure à opérer pour un simple verrou de job planifié.
 *
 * <p>{@code defaultLockAtMostFor} est un filet de sécurité : si une instance plante après
 * avoir acquis le verrou sans jamais le relâcher (crash, kill -9), le verrou expire de
 * lui-même après ce délai plutôt que de bloquer la purge indéfiniment sur toutes les
 * instances suivantes.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}
