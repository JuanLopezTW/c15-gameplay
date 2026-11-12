package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.AcertijoEnPartidaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Estado de los acertijos por partida y equipo; vive en memoria como el resto del gameplay. */
@Repository
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class AcertijosEnPartidaEnMemoria implements AcertijoEnPartidaRepository {

    private record Clave(String matchId, String equipoId, String componenteMapaId) {}

    private final Map<Clave, AcertijoEnPartida> estados = new ConcurrentHashMap<>();

    @Override
    public AcertijoEnPartida obtener(String matchId, String equipoId, String componenteMapaId) {
        return estados.computeIfAbsent(new Clave(matchId, equipoId, componenteMapaId),
                c -> new AcertijoEnPartida(c.matchId(), c.equipoId(), c.componenteMapaId()));
    }

    @Override
    public void guardar(AcertijoEnPartida acertijo) {
        estados.put(new Clave(acertijo.getMatchId(), acertijo.getEquipoId(), acertijo.getComponenteMapaId()), acertijo);
    }
}
