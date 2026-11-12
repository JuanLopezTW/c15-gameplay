package co.eci.c15.plataforma.eventos;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.common.events.SalaCerradaEvent;
import co.eci.c15.common.events.SesionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** El JSON que viaja por Kafka: es el contrato con los microservicios. */
class EventosJsonTest {

    private final ObjectMapper json = TopicosEventos.json();

    @Test
    void partidaIniciadaLlevaLaDuracionComoTextoIsoYVuelveIgual() throws Exception {
        PartidaIniciadaEvent evento = new PartidaIniciadaEvent("m1", Map.of("e1", List.of("a", "b"), "e2", List.of("c", "d")),
                Duration.ofMinutes(10));

        String texto = json.writeValueAsString(evento);

        assertTrue(texto.contains("\"duracion\":\"PT10M\""), texto);
        assertEquals(evento, json.readValue(texto, PartidaIniciadaEvent.class));
    }

    @Test
    void partidaTerminadaYSalaCerradaVuelvenIguales() throws Exception {
        MatchFinishedEvent fin = new MatchFinishedEvent("m1", "TIME_UP", null, Map.of("e1", 1, "e2", 1),
                Map.of("e1", List.of("a"), "e2", List.of("c")));
        SalaCerradaEvent cerrada = new SalaCerradaEvent("s1");

        assertEquals(fin, json.readValue(json.writeValueAsString(fin), MatchFinishedEvent.class));
        assertEquals(cerrada, json.readValue(json.writeValueAsString(cerrada), SalaCerradaEvent.class));
    }

    @Test
    void unCampoNuevoDeOtroServicioNoRompeAlQueLee() throws Exception {
        SalaCerradaEvent leido = json.readValue("{\"salaId\":\"s1\",\"motivo\":\"nuevo\"}", SalaCerradaEvent.class);

        assertEquals("s1", leido.salaId());
    }

    @Test
    void cadaEventoTieneSuTopicYSuLlave() {
        assertEquals("partida-iniciada", TopicosEventos.de(PartidaIniciadaEvent.class).nombre());
        assertEquals("s1", TopicosEventos.de(SalaCerradaEvent.class).llave().apply(new SalaCerradaEvent("s1")));
        assertEquals(MatchFinishedEvent.class, TopicosEventos.porNombre("c15.", "c15.partida-terminada").orElseThrow().tipo());
        assertTrue(TopicosEventos.porNombre("c15.", "otro.partida-terminada").isEmpty());
        assertArrayEquals(new String[]{"x.partida-iniciada", "x.partida-terminada", "x.sesion"}, KafkaEventosConfig.topics("x."));
        assertTrue(TopicosEventos.porNombre("c15.", "c15.sala-cerrada").isEmpty(), "este servicio no escucha sala-cerrada");
        assertEquals("s1", TopicosEventos.de(SesionEvent.class).llave()
                .apply(new SesionEvent("ABIERTA", "SALA", "s1", "ana", "ws-1")));
        assertThrows(IllegalArgumentException.class, () -> TopicosEventos.de(String.class));
    }
}
