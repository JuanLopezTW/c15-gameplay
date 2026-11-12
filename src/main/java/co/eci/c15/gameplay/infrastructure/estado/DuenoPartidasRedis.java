package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.application.DuenoPartidas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * El dueño de una partida tiene un turno ("lease") en Redis que vence en {@link #TURNO}. Cada tick del reloj
 * lo renueva; si la instancia muere, el turno vence y la primera que pregunte se queda con la partida.
 */
@Component
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class DuenoPartidasRedis implements DuenoPartidas {

    static final Duration TURNO = Duration.ofSeconds(10);

    /** Renueva si es mio; si no hay dueño, lo toma. */
    private static final RedisScript<Long> TOMAR_O_RENOVAR = RedisScript.of("""
            local actual = redis.call('GET', KEYS[1])
            if actual == ARGV[1] then
              redis.call('PEXPIRE', KEYS[1], ARGV[2])
              return 1
            end
            if not actual then
              redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
              return 2
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final String instancia;

    /** Se identifica con el nombre del contenedor (HOSTNAME) para saber en los logs y en Redis quien es el dueño. */
    @Autowired
    public DuenoPartidasRedis(StringRedisTemplate redis) {
        this(redis, Optional.ofNullable(System.getenv("HOSTNAME")).orElse("instancia")
                + ":" + UUID.randomUUID().toString().substring(0, 8));
    }

    DuenoPartidasRedis(StringRedisTemplate redis, String instancia) {
        this.redis = redis;
        this.instancia = instancia;
    }

    static String clave(String matchId) {
        return EstadoPartidaRedis.PREFIJO + matchId + ":dueno";
    }

    @Override
    public boolean esMia(String matchId) {
        Long r = redis.execute(TOMAR_O_RENOVAR, List.of(clave(matchId)), instancia, String.valueOf(TURNO.toMillis()));
        return !Objects.equals(r, 0L);
    }
}
