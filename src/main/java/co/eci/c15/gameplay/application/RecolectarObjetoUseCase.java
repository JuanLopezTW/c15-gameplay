package co.eci.c15.gameplay.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.InventarioEquipo;
import co.eci.c15.gameplay.domain.InventarioRepository;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.ObjetoInventario;
import co.eci.c15.gameplay.domain.ObjetoLejosException;
import co.eci.c15.gameplay.domain.ObjetoNoRecolectableException;
import co.eci.c15.gameplay.domain.ObjetoYaRecolectadoException;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.domain.TipoComponente;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recoleccion de objetos del mapa e inventario compartido por equipo (HU-31.5). El jugador debe
 * estar junto al objeto; lo que recolecta pasa al inventario del equipo completo, y cada cambio se
 * avisa en tiempo real a los miembros del equipo.
 */
@Service
public class RecolectarObjetoUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecolectarObjetoUseCase.class);

    private final JugadorEnMapaRepository jugadores;
    private final ObtenerMapaUseCase obtenerMapa;
    private final InventarioRepository inventarios;
    private final InventarioNotifier notifier;
    private final CandadoEquipo candado;

    public RecolectarObjetoUseCase(JugadorEnMapaRepository jugadores, ObtenerMapaUseCase obtenerMapa,
                                   InventarioRepository inventarios, InventarioNotifier notifier,
                                   CandadoEquipo candado) {
        this.jugadores = jugadores;
        this.obtenerMapa = obtenerMapa;
        this.inventarios = inventarios;
        this.notifier = notifier;
        this.candado = candado;
    }

    public InventarioDto ejecutar(String matchId, String userId, String componenteMapaId) {
        JugadorEnMapa jugador = jugadores.find(matchId, userId)
                .orElseThrow(() -> new JugadorNoRegistradoException(matchId, userId));
        ComponenteMapa componente = obtenerMapa.obtener(matchId).getComponentes().stream()
                .filter(c -> c.id().equals(componenteMapaId) && c.tipo() == TipoComponente.LLAVE)
                .findFirst()
                .orElseThrow(() -> new ObjetoNoRecolectableException(componenteMapaId));
        if (!estanJuntos(jugador.getPosicion(), componente.posicion())) {
            throw new ObjetoLejosException(componenteMapaId);
        }

        // Agregar, guardar y avisar juntos: los avisos de un equipo llegan en el mismo orden que los cambios.
        return candado.ejecutar(matchId, jugador.getEquipoId(), () -> {
            InventarioEquipo inventario = inventarios.obtener(matchId, jugador.getEquipoId());
            synchronized (inventario) {
                if (!inventario.agregar(new ObjetoInventario(componenteMapaId, nombreDe(componenteMapaId), userId))) {
                    throw new ObjetoYaRecolectadoException(componenteMapaId);
                }
                inventarios.guardar(inventario);
                EventoNegocio.de(log, "objeto.recolectado").con("matchId", matchId).con("equipoId", jugador.getEquipoId())
                        .con("objeto", componenteMapaId).con("userId", userId).info();
                InventarioDto foto = InventarioDto.from(inventario);
                notifier.notificar(foto);
                return foto;
            }
        });
    }

    private static boolean estanJuntos(Posicion jugador, Posicion objeto) {
        return Math.abs(jugador.x() - objeto.x()) + Math.abs(jugador.y() - objeto.y()) == 1;
    }

    /** "llave-1" -> "Llave 1". */
    static String nombreDe(String componenteMapaId) {
        String legible = componenteMapaId.replace('-', ' ');
        return Character.toUpperCase(legible.charAt(0)) + legible.substring(1);
    }
}
