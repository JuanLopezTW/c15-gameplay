package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class JugadorEnMapaRepositoryEnMemoria implements JugadorEnMapaRepository {

    private final Map<String, Map<String, JugadorEnMapa>> porPartida = new ConcurrentHashMap<>();

    @Override
    public Optional<JugadorEnMapa> find(String matchId, String userId) {
        Map<String, JugadorEnMapa> partida = porPartida.get(matchId);
        return partida == null ? Optional.empty() : Optional.ofNullable(partida.get(userId));
    }

    @Override
    public List<JugadorEnMapa> findByMatchId(String matchId) {
        Map<String, JugadorEnMapa> partida = porPartida.get(matchId);
        return partida == null ? List.of() : List.copyOf(partida.values());
    }

    @Override
    public JugadorEnMapa saveIfAbsent(JugadorEnMapa jugador) {
        JugadorEnMapa existente = porPartida
                .computeIfAbsent(jugador.getMatchId(), id -> new ConcurrentHashMap<>())
                .putIfAbsent(jugador.getUserId(), jugador);
        return existente != null ? existente : jugador;
    }

    /** El objeto guardado es el mismo que se modifico; solo se repone si alguien lo reemplazo. */
    @Override
    public void guardar(JugadorEnMapa jugador) {
        Map<String, JugadorEnMapa> partida = porPartida.get(jugador.getMatchId());
        if (partida != null) partida.computeIfPresent(jugador.getUserId(), (id, actual) -> jugador);
    }

    @Override
    public Optional<JugadorEnMapa> remove(String matchId, String userId) {
        Map<String, JugadorEnMapa> partida = porPartida.get(matchId);
        return partida == null ? Optional.empty() : Optional.ofNullable(partida.remove(userId));
    }
}
