package com.api.identity.services.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate limiting de ventana fija respaldado por Redis — el único consumidor de la conexión Redis
 * ya provisionada en prod/dev que hasta ahora no tenía ningún código que la usara.
 * INCR es atómico en Redis, así que múltiples instancias de la app comparten el mismo contador
 * sin condiciones de carrera; el TTL se pone solo la primera vez que se ve la key en la ventana,
 * para que el contador se resetee solo sin un job de limpieza aparte.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    /**
     * @return true si la operación identificada por {@code key} puede proceder sin superar
     * {@code maxRequests} invocaciones dentro de la ventana {@code window}.
     */
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                // Falla de conexión a Redis: dejamos pasar el request en vez de romper la
                // funcionalidad principal por un limiter caído — no es una garantía de seguridad
                // crítica, es protección contra abuso/spam.
                log.warn("Rate limiter: Redis no respondió para la key '{}', dejando pasar sin límite", key);
                return true;
            }
            if (count == 1L) {
                redisTemplate.expire(key, window);
            }
            return count <= maxRequests;
        } catch (RuntimeException e) {
            log.warn("Rate limiter: error consultando Redis para la key '{}', dejando pasar sin límite", key, e);
            return true;
        }
    }
}
