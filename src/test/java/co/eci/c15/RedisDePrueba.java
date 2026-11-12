package co.eci.c15;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;

/**
 * Un solo Redis en Docker para todos los flujos que corren con el estado compartido (c15.estado.redis=true).
 * Se arranca la primera vez que se pide y Testcontainers lo apaga al terminar.
 */
final class RedisDePrueba {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.8.2-alpine").withExposedPorts(6379);

    private RedisDePrueba() {
    }

    static void propiedades(DynamicPropertyRegistry propiedades) {
        if (!REDIS.isRunning()) REDIS.start();
        propiedades.add("c15.estado.redis", () -> "true");
        propiedades.add("spring.data.redis.host", REDIS::getHost);
        propiedades.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
