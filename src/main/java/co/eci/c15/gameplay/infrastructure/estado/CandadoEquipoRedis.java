package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.application.CandadoEquipo;
import co.eci.c15.gameplay.application.PartidaOcupadaException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Candado corto en Redis por partida y equipo ({@code SET NX} con vencimiento). Si la instancia que lo tiene
 * muere, vence solo en {@link #VIDA}. Se libera con un script que solo borra si el candado sigue siendo
 * propio. Es reentrante dentro del mismo hilo (abrir un acertijo consulta a un companero del mismo equipo).
 */
@Component
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class CandadoEquipoRedis implements CandadoEquipo {

    static final Duration VIDA = Duration.ofSeconds(5);
    static final Duration ESPERA_MAXIMA = Duration.ofSeconds(3);
    private static final long PAUSA_MS = 10;

    private static final RedisScript<Long> LIBERAR = RedisScript.of("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ThreadLocal<Set<String>> propios = ThreadLocal.withInitial(HashSet::new);

    public CandadoEquipoRedis(StringRedisTemplate redis) {
        this.redis = redis;
    }

    static String clave(String matchId, String ambito) {
        return EstadoPartidaRedis.PREFIJO + matchId + ":candado:" + ambito;
    }

    @Override
    public <T> T ejecutar(String matchId, String ambito, Supplier<T> accion) {
        String clave = clave(matchId, ambito);
        if (propios.get().contains(clave)) return accion.get();

        String token = UUID.randomUUID().toString();
        tomar(matchId, clave, token);
        propios.get().add(clave);
        try {
            return accion.get();
        } finally {
            Set<String> delHilo = propios.get();
            delHilo.remove(clave);
            // Los hilos de Tomcat se reutilizan: no dejar nada colgado en el hilo
            if (delHilo.isEmpty()) propios.remove();
            redis.execute(LIBERAR, List.of(clave), token);
        }
    }

    private void tomar(String matchId, String clave, String token) {
        long limite = System.nanoTime() + ESPERA_MAXIMA.toNanos();
        while (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(clave, token, VIDA))) {
            if (System.nanoTime() > limite) throw new PartidaOcupadaException(matchId);
            try {
                Thread.sleep(PAUSA_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PartidaOcupadaException(matchId);
            }
        }
    }
}
