package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.application.CronometrosPartida;
import co.eci.c15.gameplay.domain.MatchTimer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * El cronometro de cada partida es solo su inicio y su duracion ({@code c15:partida:{m}:cronometro}); las
 * partidas con cronometro corriendo estan en {@link #ACTIVOS} para que el reloj de cualquier instancia las
 * encuentre. Quitarlo es un {@code DEL}: solo una instancia recibe "lo quite" y procesa el fin.
 */
@Component
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class CronometrosRedis implements CronometrosPartida {

    static final String ACTIVOS = "c15:partidas:cronometros";

    record Guardado(String matchId, Instant inicio, Duration duracion) {}

    private final EstadoPartidaRedis estado;

    public CronometrosRedis(EstadoPartidaRedis estado) {
        this.estado = estado;
    }

    private static String clave(String matchId) {
        return EstadoPartidaRedis.clave(matchId, "cronometro");
    }

    @Override
    public boolean crear(MatchTimer cronometro) {
        StringRedisTemplate redis = estado.redis();
        String json = estado.aJson(new Guardado(cronometro.getMatchId(), cronometro.getStartedAt(), cronometro.getDuration()));
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(clave(cronometro.getMatchId()), json, estado.vida()))) {
            return false;
        }
        redis.opsForSet().add(ACTIVOS, cronometro.getMatchId());
        redis.expire(ACTIVOS, estado.vida());
        return true;
    }

    @Override
    public Optional<MatchTimer> find(String matchId) {
        return Optional.ofNullable(estado.redis().opsForValue().get(clave(matchId)))
                .map(json -> estado.deJson(json, Guardado.class))
                .map(g -> new MatchTimer(g.matchId(), g.inicio(), g.duracion()));
    }

    @Override
    public boolean quitar(String matchId) {
        boolean quitado = Boolean.TRUE.equals(estado.redis().delete(clave(matchId)));
        estado.redis().opsForSet().remove(ACTIVOS, matchId);
        return quitado;
    }

    /** Los que ya no tienen cronometro (caduco con la instancia muerta) se limpian de la lista. */
    @Override
    public List<String> activos() {
        Set<String> miembros = estado.redis().opsForSet().members(ACTIVOS);
        if (miembros == null) return List.of();
        return miembros.stream().filter(m -> {
            boolean existe = Boolean.TRUE.equals(estado.redis().hasKey(clave(m)));
            if (!existe) estado.redis().opsForSet().remove(ACTIVOS, m);
            return existe;
        }).toList();
    }
}
