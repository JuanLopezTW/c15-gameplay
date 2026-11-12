package co.eci.c15;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lo que llega de los otros servicios por Kafka con el JSON del contrato: el inicio que publica salas y las
 * sesiones del broker (no como clases Java, sino como las publicarian ellos).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class EventosKafkaFlujoTest {

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry propiedades) {
        KafkaDePrueba.propiedades(propiedades, "eventosKafka");
    }

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private MockMvc mvc;

    @Test
    void elInicioDeSalasArmaLaPartidaYAlCerrarLaConexionDelMapaElJugadorSale() throws Exception {
        kafka.send("c15.eventosKafka.partida-iniciada", "k-m1", "{\"matchId\":\"k-m1\","
                + "\"equipos\":{\"k-e1\":[\"k-ana\",\"k-beto\"],\"k-e2\":[\"k-caro\",\"k-dani\"]},\"duracion\":\"PT10M\",\"campoNuevo\":1}");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> mvc.perform(get("/api/partidas/k-m1/cronometro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segundosRestantes", allOf(greaterThan(590), lessThanOrEqualTo(600)))));
        mvc.perform(get("/api/partidas/k-m1/equipos/k-e1/posiciones").param("userId", "k-beto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", hasItem("k-ana")));

        kafka.send("c15.eventosKafka.sesion", "k-m1",
                "{\"estado\":\"CERRADA\",\"ambito\":\"PARTIDA\",\"id\":\"k-m1\",\"userId\":\"k-ana\",\"sessionId\":\"ws-ana\"}");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mvc.perform(get("/api/partidas/k-m1/equipos/k-e1/posiciones").param("userId", "k-beto"))
                        .andExpect(jsonPath("$[*].userId", not(hasItem("k-ana")))));
    }
}
