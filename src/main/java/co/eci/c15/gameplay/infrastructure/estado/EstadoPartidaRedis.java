package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Estado vivo de una partida en Redis: un hash por tipo de dato bajo {@code c15:partida:{matchId}:...}, con
 * cada valor en JSON. Toda escritura renueva la caducidad (duracion maxima de una partida + margen), asi que nada
 * queda para siempre aunque una instancia muera a mitad de partida.
 */
@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
public class EstadoPartidaRedis {

    static final String PREFIJO = "c15:partida:";

    /** Partes de una partida, para acortarles la vida al terminar. */
    static final List<String> PARTES = List.of("mapa", "jugadores", "acertijos", "inventarios", "progreso", "roles",
            "cronometro", "reto", "reto:jugadores", "reto:puntajes", "equipos", "retosGanados", "resultado");

    private static final RedisScript<Long> GUARDAR_SI_EXISTE = RedisScript.of("""
            if redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1 then
              redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
              redis.call('PEXPIRE', KEYS[1], ARGV[3])
              return 1
            end
            return 0
            """, Long.class);

    private static final RedisScript<String> QUITAR = RedisScript.of("""
            local valor = redis.call('HGET', KEYS[1], ARGV[1])
            if valor then redis.call('HDEL', KEYS[1], ARGV[1]) end
            return valor
            """, String.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final Duration vida;

    public EstadoPartidaRedis(StringRedisTemplate redis, ObjectMapper json,
                              @Value("${c15.estado.margen:10m}") Duration margen) {
        this.redis = redis;
        this.json = json;
        this.vida = PartidaIniciadaEvent.DURACION_MAXIMA.plus(margen);
    }

    static String clave(String matchId, String parte) {
        return PREFIJO + matchId + ":" + parte;
    }

    Duration vida() {
        return vida;
    }

    StringRedisTemplate redis() {
        return redis;
    }

    <T> Optional<T> leer(String clave, String campo, Class<T> tipo) {
        Object guardado = redis.opsForHash().get(clave, campo);
        return Optional.ofNullable(guardado).map(v -> deJson((String) v, tipo));
    }

    <T> List<T> todos(String clave, Class<T> tipo) {
        return redis.opsForHash().values(clave).stream().map(v -> deJson((String) v, tipo)).toList();
    }

    void guardar(String clave, String campo, Object valor) {
        redis.opsForHash().put(clave, campo, aJson(valor));
        redis.expire(clave, vida);
    }

    /** @return true si lo guardo; false si ya habia un valor (gana el primero, en cualquier instancia) */
    boolean guardarSiNoExiste(String clave, String campo, Object valor) {
        boolean nuevo = Boolean.TRUE.equals(redis.opsForHash().putIfAbsent(clave, campo, aJson(valor)));
        redis.expire(clave, vida);
        return nuevo;
    }

    /** Guarda solo si el campo sigue ahi (un jugador que salio no vuelve a aparecer). */
    boolean guardarSiExiste(String clave, String campo, Object valor) {
        Long guardado = redis.execute(GUARDAR_SI_EXISTE, List.of(clave), campo, aJson(valor),
                String.valueOf(vida.toMillis()));
        return Objects.equals(guardado, 1L);
    }

    /** Lee y borra en un solo paso; vacio si ya no estaba. */
    <T> Optional<T> quitar(String clave, String campo, Class<T> tipo) {
        return Optional.ofNullable(redis.execute(QUITAR, List.of(clave), campo)).map(v -> deJson(v, tipo));
    }

    /** Al terminar la partida lo que queda vive solo unos minutos mas (lo justo para el resumen). */
    void acortar(String matchId, Duration restante) {
        PARTES.forEach(parte -> redis.expire(clave(matchId, parte), restante));
    }

    String aJson(Object valor) {
        try {
            return json.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar el estado de la partida", e);
        }
    }

    <T> T deJson(String guardado, TypeReference<T> tipo) {
        try {
            return json.readValue(guardado, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Estado de partida corrupto en Redis", e);
        }
    }

    <T> T deJson(String guardado, Class<T> tipo) {
        try {
            return json.readValue(guardado, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Estado de partida corrupto en Redis", e);
        }
    }
}
