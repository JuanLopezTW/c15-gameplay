package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.Posicion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;

@Repository
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class JugadorEnMapaRedis implements JugadorEnMapaRepository {

    record Guardado(String matchId, String equipoId, String userId, Posicion posicion, String acertijoAbierto) {
        static Guardado de(JugadorEnMapa j) {
            return new Guardado(j.getMatchId(), j.getEquipoId(), j.getUserId(), j.getPosicion(),
                    j.getAcertijoAbierto().orElse(null));
        }

        JugadorEnMapa restaurar() {
            return JugadorEnMapa.restaurar(matchId, equipoId, userId, posicion, acertijoAbierto);
        }
    }

    private final EstadoPartidaRedis estado;

    public JugadorEnMapaRedis(EstadoPartidaRedis estado) {
        this.estado = estado;
    }

    private static String clave(String matchId) {
        return EstadoPartidaRedis.clave(matchId, "jugadores");
    }

    @Override
    public Optional<JugadorEnMapa> find(String matchId, String userId) {
        return estado.leer(clave(matchId), userId, Guardado.class).map(Guardado::restaurar);
    }

    @Override
    public List<JugadorEnMapa> findByMatchId(String matchId) {
        return estado.todos(clave(matchId), Guardado.class).stream().map(Guardado::restaurar).toList();
    }

    @Override
    public JugadorEnMapa saveIfAbsent(JugadorEnMapa jugador) {
        if (estado.guardarSiNoExiste(clave(jugador.getMatchId()), jugador.getUserId(), Guardado.de(jugador))) {
            return jugador;
        }
        return find(jugador.getMatchId(), jugador.getUserId()).orElse(jugador);
    }

    @Override
    public void guardar(JugadorEnMapa jugador) {
        estado.guardarSiExiste(clave(jugador.getMatchId()), jugador.getUserId(), Guardado.de(jugador));
    }

    @Override
    public Optional<JugadorEnMapa> remove(String matchId, String userId) {
        return estado.quitar(clave(matchId), userId, Guardado.class).map(Guardado::restaurar);
    }
}
