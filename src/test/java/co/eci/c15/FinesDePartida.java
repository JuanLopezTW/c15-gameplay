package co.eci.c15;

import co.eci.c15.common.events.MatchFinishedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * En las pruebas, lo que recibirian salas y social: los fines de partida que publico gameplay (en memoria o de
 * vuelta por Kafka). Aqui solo se comprueba que gameplay publica el evento con lo que ellos necesitan.
 */
@Component
class FinesDePartida {

    private final Map<String, MatchFinishedEvent> recibidos = new ConcurrentHashMap<>();

    @EventListener
    void onPartidaTerminada(MatchFinishedEvent evento) {
        recibidos.put(evento.matchId(), evento);
    }

    Optional<MatchFinishedEvent> de(String matchId) {
        return Optional.ofNullable(recibidos.get(matchId));
    }
}
