package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.AcertijoEnPartidaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.List;

/** Estado de cada acertijo por equipo: un campo {@code equipoId|componenteMapaId} del hash de la partida. */
@Repository
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class AcertijosEnPartidaRedis implements AcertijoEnPartidaRepository {

    record Guardado(String matchId, String equipoId, String componenteMapaId, AcertijoEnPartida.Estado estado,
                    String poseedor, Instant bloqueadoEn, List<String> entrada) {
        static Guardado de(AcertijoEnPartida a) {
            return new Guardado(a.getMatchId(), a.getEquipoId(), a.getComponenteMapaId(), a.getEstado(),
                    a.getPoseedor().orElse(null), a.getBloqueadoEn().orElse(null), a.getEntrada());
        }

        AcertijoEnPartida restaurar() {
            return AcertijoEnPartida.restaurar(matchId, equipoId, componenteMapaId, estado, poseedor, bloqueadoEn,
                    entrada == null ? List.of() : entrada);
        }
    }

    private final EstadoPartidaRedis estado;

    public AcertijosEnPartidaRedis(EstadoPartidaRedis estado) {
        this.estado = estado;
    }

    private static String clave(String matchId) {
        return EstadoPartidaRedis.clave(matchId, "acertijos");
    }

    private static String campo(String equipoId, String componenteMapaId) {
        return equipoId + "|" + componenteMapaId;
    }

    @Override
    public AcertijoEnPartida obtener(String matchId, String equipoId, String componenteMapaId) {
        String clave = clave(matchId);
        String campo = campo(equipoId, componenteMapaId);
        return estado.leer(clave, campo, Guardado.class).map(Guardado::restaurar).orElseGet(() -> {
            AcertijoEnPartida nuevo = new AcertijoEnPartida(matchId, equipoId, componenteMapaId);
            if (estado.guardarSiNoExiste(clave, campo, Guardado.de(nuevo))) return nuevo;
            return estado.leer(clave, campo, Guardado.class).map(Guardado::restaurar).orElse(nuevo);
        });
    }

    @Override
    public void guardar(AcertijoEnPartida acertijo) {
        estado.guardar(clave(acertijo.getMatchId()), campo(acertijo.getEquipoId(), acertijo.getComponenteMapaId()),
                Guardado.de(acertijo));
    }
}
