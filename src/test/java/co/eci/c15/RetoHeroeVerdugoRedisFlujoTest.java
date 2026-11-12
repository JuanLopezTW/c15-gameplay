package co.eci.c15;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Los mismos flujos de {@link RetoHeroeVerdugoFlujoTest} con el estado de la partida en Redis (como con varias instancias). */
@Testcontainers(disabledWithoutDocker = true)
class RetoHeroeVerdugoRedisFlujoTest extends RetoHeroeVerdugoFlujoTest {

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry propiedades) {
        RedisDePrueba.propiedades(propiedades);
    }
}
