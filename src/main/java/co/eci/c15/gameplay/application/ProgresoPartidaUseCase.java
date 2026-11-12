package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
import co.eci.c15.gameplay.domain.ProgresoEquipo;
import co.eci.c15.gameplay.domain.ProgresoRepository;
import co.eci.c15.gameplay.domain.TipoComponente;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Progreso de puzzles en vivo (HU-33.5). Cada acertijo resuelto suma uno al equipo, sobre el
 * total de acertijos del mapa de la partida, y todos reciben la foto actualizada de los equipos.
 * Los cambios de una misma partida se aplican y avisan de a uno, para que nadie reciba un
 * estado viejo despues de uno nuevo.
 */
@Service
public class ProgresoPartidaUseCase {

    private final ProgresoRepository progresos;
    private final ObtenerMapaUseCase obtenerMapa;
    private final ProgresoNotifier notifier;
    private final CandadoEquipo candado;
    /** Dentro de esta instancia (con el estado en memoria el candado no hace nada). */
    private final Map<String, Object> bloqueos = new ConcurrentHashMap<>();

    public ProgresoPartidaUseCase(ProgresoRepository progresos, ObtenerMapaUseCase obtenerMapa, ProgresoNotifier notifier,
                                  CandadoEquipo candado) {
        this.progresos = progresos;
        this.obtenerMapa = obtenerMapa;
        this.notifier = notifier;
        this.candado = candado;
    }

    /** Al iniciar la partida todos los equipos aparecen en 0 sobre el total del mapa. */
    public void iniciar(String matchId, Collection<String> equipoIds) {
        int total = totalAcertijos(matchId);
        candado.ejecutar(matchId, CandadoEquipo.PARTIDA, () -> {
            synchronized (bloqueo(matchId)) {
                equipoIds.forEach(equipoId -> progresos.obtener(matchId, equipoId, total));
                notifier.notificar(foto(matchId));
            }
        });
    }

    /** Suma el acertijo al equipo; si ya estaba contado no se vuelve a avisar. */
    public void acertijoResuelto(AcertijoResueltoEvent evento) {
        int total = totalAcertijos(evento.matchId());
        candado.ejecutar(evento.matchId(), CandadoEquipo.PARTIDA, () -> {
            synchronized (bloqueo(evento.matchId())) {
                ProgresoEquipo progreso = progresos.obtener(evento.matchId(), evento.equipoId(), total);
                if (progreso.registrarResuelto(evento.componenteMapaId())) {
                    progresos.guardar(progreso);
                    notifier.notificar(foto(evento.matchId()));
                }
            }
        });
    }

    public ProgresoPartidaDto consultar(String matchId) {
        return foto(matchId);
    }

    private ProgresoPartidaDto foto(String matchId) {
        return ProgresoPartidaDto.from(matchId, progresos.findByMatchId(matchId));
    }

    private int totalAcertijos(String matchId) {
        return (int) obtenerMapa.obtener(matchId).getComponentes().stream()
                .filter(c -> c.tipo() == TipoComponente.ACERTIJO).count();
    }

    private Object bloqueo(String matchId) {
        return bloqueos.computeIfAbsent(matchId, id -> new Object());
    }
}
