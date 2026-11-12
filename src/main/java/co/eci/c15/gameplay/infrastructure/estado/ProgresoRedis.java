package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.domain.ProgresoEquipo;
import co.eci.c15.gameplay.domain.ProgresoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;

import java.util.Comparator;
import java.util.List;

@Repository
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class ProgresoRedis implements ProgresoRepository {

    record Guardado(String matchId, String equipoId, int total, List<String> resueltos) {
        static Guardado de(ProgresoEquipo p) {
            return new Guardado(p.getMatchId(), p.getEquipoId(), p.getTotal(), p.getResueltos());
        }

        ProgresoEquipo restaurar() {
            return ProgresoEquipo.restaurar(matchId, equipoId, total, resueltos == null ? List.of() : resueltos);
        }
    }

    private final EstadoPartidaRedis estado;

    public ProgresoRedis(EstadoPartidaRedis estado) {
        this.estado = estado;
    }

    private static String clave(String matchId) {
        return EstadoPartidaRedis.clave(matchId, "progreso");
    }

    /** Se guarda al crearlo: la foto del progreso muestra a todos los equipos desde el inicio, en 0. */
    @Override
    public ProgresoEquipo obtener(String matchId, String equipoId, int total) {
        String clave = clave(matchId);
        return estado.leer(clave, equipoId, Guardado.class).map(Guardado::restaurar).orElseGet(() -> {
            ProgresoEquipo nuevo = new ProgresoEquipo(matchId, equipoId, total);
            if (estado.guardarSiNoExiste(clave, equipoId, Guardado.de(nuevo))) return nuevo;
            return estado.leer(clave, equipoId, Guardado.class).map(Guardado::restaurar).orElse(nuevo);
        });
    }

    @Override
    public void guardar(ProgresoEquipo progreso) {
        estado.guardar(clave(progreso.getMatchId()), progreso.getEquipoId(), Guardado.de(progreso));
    }

    @Override
    public List<ProgresoEquipo> findByMatchId(String matchId) {
        return estado.todos(clave(matchId), Guardado.class).stream()
                .map(Guardado::restaurar)
                .sorted(Comparator.comparing(ProgresoEquipo::getEquipoId))
                .toList();
    }
}
