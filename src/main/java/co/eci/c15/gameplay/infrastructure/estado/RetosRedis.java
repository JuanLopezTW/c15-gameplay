package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.application.RetosPartida;
import co.eci.c15.gameplay.domain.ChallengeNotActiveException;
import co.eci.c15.gameplay.domain.ChallengeResult;
import co.eci.c15.gameplay.domain.NotInChallengeException;
import co.eci.c15.gameplay.domain.Rol;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * El reto de cada partida en Redis:
 * <ul>
 *   <li>{@code c15:partida:{m}:reto} (hash): estado, inicio, fin y equipos.</li>
 *   <li>{@code ...:reto:jugadores} (hash): userId -> "equipo|ROL".</li>
 *   <li>{@code ...:reto:puntajes} (hash): equipo -> puntos.</li>
 * </ul>
 * Cada click y cada cambio de estado es un script que corre entero dentro de Redis: un click revisa que el
 * reto siga EN_CURSO y suma en el mismo paso, asi que despues de TERMINADO no se cuela ninguno, aunque los
 * clicks lleguen por varias instancias.
 */
@Component
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class RetosRedis implements RetosPartida {

    private static final String CAMPO_ESTADO = "estado";

    static final String ACTIVOS = "c15:partidas:retos";
    private static final TypeReference<List<String>> LISTA = new TypeReference<>() {};

    private static final RedisScript<Long> PROGRAMAR = RedisScript.of("""
            if redis.call('HSETNX', KEYS[1], 'estado', 'PROGRAMADO') == 0 then return 0 end
            redis.call('HSET', KEYS[1], 'inicio', ARGV[1], 'equipos', ARGV[2])
            redis.call('PEXPIRE', KEYS[1], ARGV[3])
            return 1
            """, Long.class);

    /** ARGV: fin, vida(ms), y luego pares jugador, "equipo|ROL"; los equipos van en ARGV[3] separados por coma. */
    private static final RedisScript<Long> INICIAR = RedisScript.of("""
            if redis.call('HGET', KEYS[1], 'estado') ~= 'PROGRAMADO' then return 0 end
            redis.call('HSET', KEYS[1], 'estado', 'EN_CURSO', 'fin', ARGV[1])
            for equipo in string.gmatch(ARGV[3], '[^,]+') do redis.call('HSET', KEYS[3], equipo, 0) end
            for i = 4, #ARGV, 2 do redis.call('HSET', KEYS[2], ARGV[i], ARGV[i + 1]) end
            for _, k in ipairs(KEYS) do redis.call('PEXPIRE', k, ARGV[2]) end
            return 1
            """, Long.class);

    /** 1 contado, -1 no hay reto en curso o ya paso la hora, -2 el jugador no esta en el reto. */
    private static final RedisScript<Long> CLICK = RedisScript.of("""
            if redis.call('HGET', KEYS[1], 'estado') ~= 'EN_CURSO' then return -1 end
            local info = redis.call('HGET', KEYS[2], ARGV[1])
            if not info then return -2 end
            if tonumber(ARGV[2]) >= tonumber(redis.call('HGET', KEYS[1], 'fin')) then return -1 end
            local corte = string.find(info, '|', 1, true)
            local equipo = string.sub(info, 1, corte - 1)
            if string.sub(info, corte + 1) == 'HEROE' then
              redis.call('HINCRBY', KEYS[3], equipo, 1)
            else
              for _, rival in ipairs(redis.call('HKEYS', KEYS[3])) do
                if rival ~= equipo then redis.call('HINCRBY', KEYS[3], rival, -1) end
              end
            end
            return 1
            """, Long.class);

    /** Devuelve los puntajes finales (equipo, puntos, ...) o una lista vacia si no estaba en curso. */
    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> TERMINAR = RedisScript.of("""
            if redis.call('HGET', KEYS[1], 'estado') ~= 'EN_CURSO' then return {} end
            redis.call('HSET', KEYS[1], 'estado', 'TERMINADO')
            return redis.call('HGETALL', KEYS[2])
            """, List.class);

    private static final RedisScript<Long> CANCELAR = RedisScript.of("""
            if redis.call('HGET', KEYS[1], 'estado') ~= 'PROGRAMADO' then return 0 end
            redis.call('HSET', KEYS[1], 'estado', 'CANCELADO')
            return 1
            """, Long.class);

    private final EstadoPartidaRedis estado;

    public RetosRedis(EstadoPartidaRedis estado) {
        this.estado = estado;
    }

    static String clave(String matchId) {
        return EstadoPartidaRedis.clave(matchId, "reto");
    }

    private static List<String> claves(String matchId) {
        return List.of(clave(matchId), clave(matchId) + ":jugadores", clave(matchId) + ":puntajes");
    }

    private StringRedisTemplate redis() {
        return estado.redis();
    }

    @Override
    public boolean programar(String matchId, List<String> equipos, Instant inicio) {
        Long r = redis().execute(PROGRAMAR, List.of(clave(matchId)), String.valueOf(inicio.toEpochMilli()),
                estado.aJson(equipos), String.valueOf(estado.vida().toMillis()));
        if (r == null || r == 0) return false;
        redis().opsForSet().add(ACTIVOS, matchId);
        redis().expire(ACTIVOS, estado.vida());
        return true;
    }

    @Override
    public Optional<Plan> plan(String matchId) {
        Map<Object, Object> h = redis().opsForHash().entries(clave(matchId));
        if (h.isEmpty() || h.get(CAMPO_ESTADO) == null) return Optional.empty();
        List<String> equipos = estado.deJson((String) h.get("equipos"), LISTA);
        return Optional.of(new Plan(matchId, equipos, Instant.ofEpochMilli(Long.parseLong((String) h.get("inicio"))),
                Estado.valueOf((String) h.get(CAMPO_ESTADO))));
    }

    @Override
    public boolean iniciar(String matchId, Map<String, Map<String, Rol>> rolesPorEquipo, Instant fin) {
        List<String> args = new ArrayList<>(List.of(String.valueOf(fin.toEpochMilli()),
                String.valueOf(estado.vida().toMillis()), String.join(",", new TreeMap<>(rolesPorEquipo).keySet())));
        rolesPorEquipo.forEach((equipo, roles) -> roles.forEach((userId, rol) -> {
            args.add(userId);
            args.add(equipo + "|" + rol.name());
        }));
        Long r = redis().execute(INICIAR, claves(matchId), args.toArray());
        return r != null && r == 1;
    }

    @Override
    public void click(String matchId, String userId, Instant ahora) {
        Long r = redis().execute(CLICK, claves(matchId), userId, String.valueOf(ahora.toEpochMilli()));
        if (r == null || r == -1) throw new ChallengeNotActiveException(matchId);
        if (r == -2) throw new NotInChallengeException(matchId, userId);
    }

    @Override
    public Optional<EnCurso> enCurso(String matchId) {
        List<Object> estadoYFin = redis().opsForHash().multiGet(clave(matchId), List.of(CAMPO_ESTADO, "fin"));
        if (!"EN_CURSO".equals(estadoYFin.get(0))) return Optional.empty();
        Instant fin = Instant.ofEpochMilli(Long.parseLong((String) estadoYFin.get(1)));
        return Optional.of(new EnCurso(matchId, fin, puntajes(redis().opsForHash().entries(claves(matchId).get(2)))));
    }

    @Override
    public Optional<ChallengeResult> terminar(String matchId) {
        List<?> plano = redis().execute(TERMINAR, List.of(clave(matchId), claves(matchId).get(2)));
        // Sin puntajes es que no estaba en curso (otra instancia ya lo cerro)
        if (plano == null || plano.size() < 2) return Optional.empty();
        redis().opsForSet().remove(ACTIVOS, matchId);
        Map<Object, Object> pares = new TreeMap<>();
        for (int i = 0; i + 1 < plano.size(); i += 2) pares.put(plano.get(i), plano.get(i + 1));
        return Optional.of(ChallengeResult.deMarcador(matchId, puntajes(pares)));
    }

    @Override
    public void cancelar(String matchId) {
        Long r = redis().execute(CANCELAR, List.of(clave(matchId)));
        if (r != null && r == 1) redis().opsForSet().remove(ACTIVOS, matchId);
    }

    /** Los retos que ya no existen (caducaron) se limpian de la lista. */
    @Override
    public List<String> activos() {
        Set<String> miembros = redis().opsForSet().members(ACTIVOS);
        if (miembros == null) return List.of();
        return miembros.stream().filter(m -> {
            Object e = redis().opsForHash().get(clave(m), CAMPO_ESTADO);
            boolean vivo = "PROGRAMADO".equals(e) || "EN_CURSO".equals(e);
            if (!vivo) redis().opsForSet().remove(ACTIVOS, m);
            return vivo;
        }).toList();
    }

    private static Map<String, Long> puntajes(Map<Object, Object> crudos) {
        Map<String, Long> puntajes = new TreeMap<>();
        crudos.forEach((equipo, puntos) -> puntajes.put(String.valueOf(equipo), Long.parseLong(String.valueOf(puntos))));
        return puntajes;
    }
}
