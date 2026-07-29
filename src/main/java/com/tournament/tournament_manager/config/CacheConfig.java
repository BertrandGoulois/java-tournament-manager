package com.tournament.tournament_manager.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

/**
 * Configuration du cache Redis.
 *
 * <p>TTL par défaut : 10 minutes pour toutes les entrées.
 * Sérialisation : JSON, via {@code GenericJacksonJsonRedisSerializer} (Jackson 3),
 * avec une liste blanche de types autorisée à la désérialisation. Une sérialisation
 * Java native exposerait toute clé de cache écrivable (Redis, accessible sans
 * authentification par ailleurs) à une exécution de code arbitraire via {@code readObject()}.
 *
 * <p>Les DTOs mis en cache (ex. {@code PlayerStatsResponse}, {@code EloHistoryResponse})
 * n'ont pas besoin d'implémenter {@code Serializable}.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.tournament.tournament_manager.dto.response")
                .allowIfSubType("java.util")
                .build();

        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
