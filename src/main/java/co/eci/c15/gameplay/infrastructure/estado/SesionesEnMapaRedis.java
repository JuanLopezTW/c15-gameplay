package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.application.SesionesEnMapa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Compartido entre replicas: el aviso de que se abrio la conexion nueva y el de que se cerro la vieja pueden
 * llegarle a replicas distintas. Clave {@code c15:partida:{m}:sesion:{userId}} con la ultima conexion; caduca sola
 * (el broker la renueva cada 30 s mientras la conexion siga abierta).
 */
@Component
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class SesionesEnMapaRedis implements SesionesEnMapa {

    static final Duration VIDA = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;

    public SesionesEnMapaRedis(StringRedisTemplate redis) {
        this.redis = redis;
    }

    static String clave(String matchId, String userId) {
        return EstadoPartidaRedis.PREFIJO + matchId + ":sesion:" + userId;
    }

    @Override
    public void abierta(String matchId, String userId, String sessionId) {
        redis.opsForValue().set(clave(matchId, userId), sessionId, VIDA);
    }

    @Override
    public boolean esLaUltima(String matchId, String userId, String sessionId) {
        String actual = redis.opsForValue().get(clave(matchId, userId));
        return actual == null || Objects.equals(actual, sessionId);
    }
}
