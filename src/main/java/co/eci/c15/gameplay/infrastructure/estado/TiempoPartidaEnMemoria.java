package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.application.CronometrosPartida;
import co.eci.c15.gameplay.application.DuenoPartidas;
import co.eci.c15.gameplay.application.ResultadosPartida;
import co.eci.c15.gameplay.application.RetosPartida;
import co.eci.c15.gameplay.domain.ChallengeNotActiveException;
import co.eci.c15.gameplay.domain.ChallengeResult;
import co.eci.c15.gameplay.domain.HeroVillainChallenge;
import co.eci.c15.gameplay.domain.MatchResult;
import co.eci.c15.gameplay.domain.MatchTimer;
import co.eci.c15.gameplay.domain.Rol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cronometros, retos y resultados en memoria: una sola instancia, que siempre es la dueña de sus partidas.
 * Es el comportamiento de siempre (sin Redis).
 */
@Configuration
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class TiempoPartidaEnMemoria {

    @Bean
    public DuenoPartidas duenoPartidasEnMemoria() {
        return matchId -> true;
    }

    @Bean
    public CronometrosPartida cronometrosEnMemoria() {
        return new Cronometros();
    }

    @Bean
    public RetosPartida retosEnMemoria() {
        return new Retos();
    }

    @Bean
    public ResultadosPartida resultadosEnMemoria() {
        return new Resultados();
    }

    static class Cronometros implements CronometrosPartida {
        private final Map<String, MatchTimer> timers = new ConcurrentHashMap<>();

        @Override
        public boolean crear(MatchTimer cronometro) {
            return timers.putIfAbsent(cronometro.getMatchId(), cronometro) == null;
        }

        @Override
        public Optional<MatchTimer> find(String matchId) {
            return Optional.ofNullable(timers.get(matchId));
        }

        @Override
        public boolean quitar(String matchId) {
            return timers.remove(matchId) != null;
        }

        @Override
        public List<String> activos() {
            return List.copyOf(timers.keySet());
        }
    }

    /** El reto en curso es el objeto del dominio, hecho para recibir clicks en paralelo con un resultado exacto. */
    static class Retos implements RetosPartida {

        private static final class Reto {
            private final List<String> equipos;
            private final Instant inicio;
            private Estado estado = Estado.PROGRAMADO;
            private HeroVillainChallenge enCurso;

            private Reto(List<String> equipos, Instant inicio) {
                this.equipos = equipos;
                this.inicio = inicio;
            }
        }

        private final Map<String, Reto> retos = new ConcurrentHashMap<>();

        @Override
        public boolean programar(String matchId, List<String> equipos, Instant inicio) {
            return retos.putIfAbsent(matchId, new Reto(List.copyOf(equipos), inicio)) == null;
        }

        @Override
        public Optional<Plan> plan(String matchId) {
            Reto reto = retos.get(matchId);
            if (reto == null) return Optional.empty();
            synchronized (reto) {
                return Optional.of(new Plan(matchId, reto.equipos, reto.inicio, reto.estado));
            }
        }

        @Override
        public boolean iniciar(String matchId, Map<String, Map<String, Rol>> rolesPorEquipo, Instant fin) {
            Reto reto = retos.get(matchId);
            if (reto == null) return false;
            synchronized (reto) {
                if (reto.estado != Estado.PROGRAMADO) return false;
                reto.enCurso = new HeroVillainChallenge(matchId, rolesPorEquipo, fin);
                reto.estado = Estado.EN_CURSO;
                return true;
            }
        }

        @Override
        public void click(String matchId, String userId, Instant ahora) {
            HeroVillainChallenge enCurso = enCursoDe(matchId);
            if (enCurso == null) throw new ChallengeNotActiveException(matchId);
            enCurso.click(userId, ahora);
        }

        @Override
        public Optional<EnCurso> enCurso(String matchId) {
            HeroVillainChallenge reto = enCursoDe(matchId);
            return Optional.ofNullable(reto).map(r -> new EnCurso(matchId, r.getEndsAt(), r.scores()));
        }

        @Override
        public Optional<ChallengeResult> terminar(String matchId) {
            Reto reto = retos.get(matchId);
            if (reto == null) return Optional.empty();
            synchronized (reto) {
                if (reto.estado != Estado.EN_CURSO) return Optional.empty();
                reto.estado = Estado.TERMINADO;
                return Optional.of(reto.enCurso.finish());
            }
        }

        @Override
        public void cancelar(String matchId) {
            Reto reto = retos.get(matchId);
            if (reto == null) return;
            synchronized (reto) {
                if (reto.estado == Estado.PROGRAMADO) reto.estado = Estado.CANCELADO;
            }
        }

        @Override
        public List<String> activos() {
            return retos.entrySet().stream()
                    .filter(e -> {
                        synchronized (e.getValue()) {
                            Estado estado = e.getValue().estado;
                            return estado == Estado.PROGRAMADO || estado == Estado.EN_CURSO;
                        }
                    })
                    .map(Map.Entry::getKey).toList();
        }

        private HeroVillainChallenge enCursoDe(String matchId) {
            Reto reto = retos.get(matchId);
            if (reto == null) return null;
            synchronized (reto) {
                return reto.estado == Estado.EN_CURSO ? reto.enCurso : null;
            }
        }
    }

    static class Resultados implements ResultadosPartida {
        private final Map<String, Map<String, List<String>>> equipos = new ConcurrentHashMap<>();
        private final Map<String, Map<String, Integer>> retosGanados = new ConcurrentHashMap<>();
        private final Map<String, MatchResult> terminados = new ConcurrentHashMap<>();

        @Override
        public void guardarEquipos(String matchId, Map<String, List<String>> equiposDePartida) {
            equipos.putIfAbsent(matchId, Collections.unmodifiableMap(new TreeMap<>(equiposDePartida)));
        }

        @Override
        public Optional<Map<String, List<String>>> equipos(String matchId) {
            return Optional.ofNullable(equipos.get(matchId));
        }

        @Override
        public void sumarRetoGanado(String matchId, String equipoId) {
            retosGanados.computeIfAbsent(matchId, id -> new ConcurrentHashMap<>()).merge(equipoId, 1, Integer::sum);
        }

        @Override
        public Map<String, Integer> retosGanados(String matchId) {
            return Map.copyOf(retosGanados.getOrDefault(matchId, Map.of()));
        }

        @Override
        public boolean guardar(MatchResult resultado) {
            return terminados.putIfAbsent(resultado.matchId(), resultado) == null;
        }

        @Override
        public Optional<MatchResult> find(String matchId) {
            return Optional.ofNullable(terminados.get(matchId));
        }
    }
}
