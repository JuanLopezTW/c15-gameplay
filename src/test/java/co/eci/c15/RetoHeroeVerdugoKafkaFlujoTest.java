package co.eci.c15;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Los mismos flujos de {@link RetoHeroeVerdugoFlujoTest} con los eventos entre servicios por Kafka (como entre microservicios). */
@Testcontainers(disabledWithoutDocker = true)
class RetoHeroeVerdugoKafkaFlujoTest extends RetoHeroeVerdugoFlujoTest {

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry propiedades) {
        KafkaDePrueba.propiedades(propiedades, "retoHeroeVerdugo");
    }
}
