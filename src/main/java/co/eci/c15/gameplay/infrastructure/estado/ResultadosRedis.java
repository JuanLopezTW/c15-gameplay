package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.application.ResultadosPartida;
import co.eci.c15.gameplay.domain.MatchResult;
import co.eci.c15.gameplay.domain.TeamScore;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Equipos, retos ganados y resultado final de cada partida, en {@code c15:partida:{m}:...}. */
@Component
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class ResultadosRedis implements ResultadosPartida {

    private static final TypeReference<TreeMap<String, List<String>>> EQUIPOS = new TypeReference<>() {};

    /** Sin las propiedades calculadas del record (como "empate"), que no se pueden volver a leer. */
    record Guardado(String matchId, MatchResult.Reason motivo, String ganador, List<String> empatados,
                    List<TeamScore> clasificacion) {
        static Guardado de(MatchResult r) {
            return new Guardado(r.matchId(), r.reason(), r.winnerTeamId(), r.tiedTeamIds(), r.standings());
        }

        MatchResult restaurar() {
            return new MatchResult(matchId, motivo, ganador, empatados, clasificacion);
        }
    }

    private final EstadoPartidaRedis estado;

    public ResultadosRedis(EstadoPartidaRedis estado) {
        this.estado = estado;
    }

    private static String clave(String matchId, String parte) {
        return EstadoPartidaRedis.clave(matchId, parte);
    }

    @Override
    public void guardarEquipos(String matchId, Map<String, List<String>> equipos) {
        estado.redis().opsForValue().setIfAbsent(clave(matchId, "equipos"), estado.aJson(new TreeMap<>(equipos)),
                estado.vida());
    }

    @Override
    public Optional<Map<String, List<String>>> equipos(String matchId) {
        return Optional.ofNullable(estado.redis().opsForValue().get(clave(matchId, "equipos")))
                .map(json -> estado.deJson(json, EQUIPOS));
    }

    @Override
    public void sumarRetoGanado(String matchId, String equipoId) {
        String clave = clave(matchId, "retosGanados");
        estado.redis().opsForHash().increment(clave, equipoId, 1);
        estado.redis().expire(clave, estado.vida());
    }

    @Override
    public Map<String, Integer> retosGanados(String matchId) {
        Map<String, Integer> ganados = new TreeMap<>();
        estado.redis().opsForHash().entries(clave(matchId, "retosGanados"))
                .forEach((equipo, n) -> ganados.put(String.valueOf(equipo), Integer.parseInt(String.valueOf(n))));
        return ganados;
    }

    @Override
    public boolean guardar(MatchResult resultado) {
        return Boolean.TRUE.equals(estado.redis().opsForValue().setIfAbsent(clave(resultado.matchId(), "resultado"),
                estado.aJson(Guardado.de(resultado)), estado.vida()));
    }

    @Override
    public Optional<MatchResult> find(String matchId) {
        return Optional.ofNullable(estado.redis().opsForValue().get(clave(matchId, "resultado")))
                .map(json -> estado.deJson(json, Guardado.class).restaurar());
    }
}
