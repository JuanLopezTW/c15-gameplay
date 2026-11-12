package co.eci.c15;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Un solo Kafka en Docker para los flujos que corren con los eventos entre servicios por Kafka
 * (c15.eventos.kafka=true). Cada clase usa sus propios topics (prefijo) para no leer eventos de otra.
 */
final class KafkaDePrueba {

    @SuppressWarnings("resource")
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.1.0");

    private KafkaDePrueba() {
    }

    static void propiedades(DynamicPropertyRegistry propiedades, String clase) {
        if (!KAFKA.isRunning()) KAFKA.start();
        propiedades.add("c15.eventos.kafka", () -> "true");
        propiedades.add("c15.eventos.prefijo", () -> "c15." + clase + ".");
        propiedades.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        // Los topics son nuevos en cada clase: se leen desde el principio
        propiedades.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }
}
