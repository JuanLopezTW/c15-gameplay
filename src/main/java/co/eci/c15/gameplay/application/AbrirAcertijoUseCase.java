package co.eci.c15.gameplay.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.AcertijoEnPartidaRepository;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.ElementoInteractivoRepository;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.TipoComponente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abrir y cerrar acertijos del mapa (HU-27.5, 35.5, 66.5). Cada equipo tiene su propio estado
 * de cada acertijo; mientras un jugador lo tiene abierto, el resto de su equipo no puede abrirlo
 * y él no puede moverse. El bloqueo se libera al cerrar, si el jugador sale de la partida
 * (desconexión) o tras {@link #TTL_BLOQUEO} como respaldo.
 */
@Service
public class AbrirAcertijoUseCase {

    private static final Logger log = LoggerFactory.getLogger(AbrirAcertijoUseCase.class);

    public static final Duration TTL_BLOQUEO = Duration.ofMinutes(5);

    private final JugadorEnMapaRepository jugadores;
    private final ObtenerMapaUseCase obtenerMapa;
    private final AcertijoRepository acertijos;
    private final ElementoInteractivoRepository elementos;
    private final AcertijoEnPartidaRepository estados;
    private final CandadoEquipo candado;
    private final Clock clock;

    @Autowired
    public AbrirAcertijoUseCase(JugadorEnMapaRepository jugadores, ObtenerMapaUseCase obtenerMapa,
                                AcertijoRepository acertijos, ElementoInteractivoRepository elementos,
                                AcertijoEnPartidaRepository estados, CandadoEquipo candado) {
        this(jugadores, obtenerMapa, acertijos, elementos, estados, candado, Clock.systemUTC());
    }

    AbrirAcertijoUseCase(JugadorEnMapaRepository jugadores, ObtenerMapaUseCase obtenerMapa,
                         AcertijoRepository acertijos, ElementoInteractivoRepository elementos,
                         AcertijoEnPartidaRepository estados, CandadoEquipo candado, Clock clock) {
        this.jugadores = jugadores;
        this.obtenerMapa = obtenerMapa;
        this.acertijos = acertijos;
        this.elementos = elementos;
        this.estados = estados;
        this.candado = candado;
        this.clock = clock;
    }

    public VisualizarAcertijoDto ejecutar(String matchId, String componenteMapaId, String userId) {
        String equipoId = jugadorDe(matchId, userId).getEquipoId();
        ComponenteMapa componente = obtenerMapa.obtener(matchId).getComponentes().stream()
                .filter(c -> c.id().equals(componenteMapaId) && c.tipo() == TipoComponente.ACERTIJO)
                .findFirst()
                .orElseThrow(() -> new ComponenteSinAcertijoException(componenteMapaId));
        Acertijo acertijo = acertijos.findByComponenteMapaId(componenteMapaId)
                .orElseThrow(() -> new ComponenteSinAcertijoException(componenteMapaId));
        return candado.ejecutar(matchId, equipoId, () -> {
            JugadorEnMapa jugador = jugadorDe(matchId, userId);
            AcertijoEnPartida estado = estados.obtener(matchId, equipoId, componenteMapaId);
            if (!estado.isResuelto()) {
                AtomicReference<Optional<String>> desplazado = new AtomicReference<>(Optional.empty());
                jugador.abrirAcertijo(componente, () -> desplazado.set(
                        estado.bloquear(userId, clock.instant(), TTL_BLOQUEO,
                                poseedor -> jugadores.find(matchId, poseedor).isPresent())));
                // Si el bloqueo venció y se lo quitamos a otro jugador, él puede volver a moverse.
                desplazado.get()
                        .flatMap(anterior -> jugadores.find(matchId, anterior))
                        .ifPresent(anterior -> {
                            anterior.cerrarAcertijo(componenteMapaId);
                            jugadores.guardar(anterior);
                        });
                estados.guardar(estado);
                jugadores.guardar(jugador);
                EventoNegocio.de(log, "acertijo.abierto").con("matchId", matchId).con("equipoId", equipoId)
                        .con("acertijo", componenteMapaId).con("userId", userId).info();
            }
            return VisualizarAcertijoDto.from(acertijo, estado, elementos.findByAcertijoId(acertijo.getId()));
        });
    }

    /** Cierra el acertijo: libera el bloqueo (si era suyo) y el jugador puede volver a moverse. */
    public void cerrar(String matchId, String componenteMapaId, String userId) {
        String equipoId = jugadorDe(matchId, userId).getEquipoId();
        candado.ejecutar(matchId, equipoId, () -> {
            JugadorEnMapa jugador = jugadorDe(matchId, userId);
            AcertijoEnPartida estado = estados.obtener(matchId, equipoId, componenteMapaId);
            estado.liberar(userId);
            jugador.cerrarAcertijo(componenteMapaId);
            estados.guardar(estado);
            jugadores.guardar(jugador);
        });
    }

    private JugadorEnMapa jugadorDe(String matchId, String userId) {
        return jugadores.find(matchId, userId).orElseThrow(() -> new JugadorNoRegistradoException(matchId, userId));
    }
}
