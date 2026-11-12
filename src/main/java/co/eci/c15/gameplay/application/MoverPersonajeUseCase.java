package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.Posicion;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class MoverPersonajeUseCase {

    private final JugadorEnMapaRepository jugadores;
    private final ObtenerMapaUseCase obtenerMapa;
    private final ApplicationEventPublisher events;
    private final CandadoEquipo candado;

    public MoverPersonajeUseCase(JugadorEnMapaRepository jugadores,
                                 ObtenerMapaUseCase obtenerMapa,
                                 ApplicationEventPublisher events,
                                 CandadoEquipo candado) {
        this.jugadores = jugadores;
        this.obtenerMapa = obtenerMapa;
        this.events = events;
        this.candado = candado;
    }

    public MovimientoDto ejecutar(String matchId, String userId, Posicion destino) {
        String equipoId = jugadorDe(matchId, userId).getEquipoId();
        MapaIsometrico mapa = obtenerMapa.obtener(matchId);
        // Se vuelve a leer dentro del candado: puede haberse movido desde otra instancia
        return candado.ejecutar(matchId, equipoId, () -> {
            JugadorEnMapa jugador = jugadorDe(matchId, userId);
            JugadorEnMapa.Movimiento movimiento = jugador.mover(mapa, destino);
            jugadores.guardar(jugador);
            events.publishEvent(new PosicionActualizadaEvent(matchId, equipoId, userId,
                    movimiento.hasta(), movimiento.ruta()));
            return MovimientoDto.from(userId, movimiento);
        });
    }

    private JugadorEnMapa jugadorDe(String matchId, String userId) {
        return jugadores.find(matchId, userId).orElseThrow(() -> new JugadorNoRegistradoException(matchId, userId));
    }
}
