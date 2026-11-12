package co.eci.c15.gameplay.infrastructure.estado;

import static org.awaitility.Awaitility.await;
import co.eci.c15.gameplay.application.RetosPartida;
import co.eci.c15.gameplay.domain.ChallengeNotActiveException;
import co.eci.c15.gameplay.domain.ChallengeResult;
import co.eci.c15.gameplay.domain.MatchResult;
import co.eci.c15.gameplay.domain.MatchTimer;
import co.eci.c15.gameplay.domain.NotInChallengeException;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.TeamScore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Cronometro, dueño, reto y resultado contra un Redis real, con dos "instancias" cuando importa. */
class TiempoPartidaRedisTest extends RedisDePrueba {

    private static final Instant AHORA = Instant.parse("2026-11-03T15:00:00Z");
    private static final Map<String, Map<String, Rol>> ROLES = Map.of(
            "A", Map.of("ana", Rol.HEROE, "beto", Rol.VERDUGO),
            "B", Map.of("caro", Rol.HEROE, "dani", Rol.VERDUGO));

    // ---------- dueño ----------

    @Test
    void elPrimeroTomaLaPartidaLaRenuevaYSiMuereOtroLaToma() {
        DuenoPartidasRedis instanciaA = new DuenoPartidasRedis(redis, "A");
        DuenoPartidasRedis instanciaB = new DuenoPartidasRedis(redis, "B");

        assertTrue(instanciaA.esMia("m1"));
        assertFalse(instanciaB.esMia("m1"));
        assertTrue(instanciaA.esMia("m1"), "renueva su turno");
        assertTrue(instanciaB.esMia("m2"), "cada partida tiene su dueño");
        long turno = redis.getExpire(DuenoPartidasRedis.clave("m1"), TimeUnit.MILLISECONDS);
        assertTrue(turno > 0 && turno <= DuenoPartidasRedis.TURNO.toMillis(), "vence en " + turno);

        redis.delete(DuenoPartidasRedis.clave("m1")); // A murio y su turno vencio
        assertTrue(instanciaB.esMia("m1"));
        assertFalse(instanciaA.esMia("m1"));
    }

    // ---------- cronometro ----------

    @Test
    void elCronometroSeCreaUnaVezSeLeeIgualYSeQuitaUnaVez() {
        CronometrosRedis cronometros = new CronometrosRedis(estado);
        MatchTimer cinco = new MatchTimer("m1", AHORA, Duration.ofMinutes(5));

        assertTrue(cronometros.crear(cinco));
        assertFalse(cronometros.crear(new MatchTimer("m1", AHORA.plusSeconds(9), Duration.ofMinutes(1))));
        MatchTimer leido = cronometros.find("m1").orElseThrow();
        assertEquals(AHORA, leido.getStartedAt());
        assertEquals(Duration.ofMinutes(5), leido.getDuration());
        assertEquals(List.of("m1"), cronometros.activos());

        assertTrue(cronometros.quitar("m1"));
        assertFalse(cronometros.quitar("m1"), "el fin se procesa una sola vez");
        assertTrue(cronometros.find("m1").isEmpty());
        assertTrue(cronometros.activos().isEmpty());
    }

    @Test
    void unCronometroQueCaducoSaleDeLaListaDeActivos() {
        CronometrosRedis cronometros = new CronometrosRedis(estado);
        cronometros.crear(new MatchTimer("m1", AHORA, Duration.ofMinutes(5)));

        redis.delete(EstadoPartidaRedis.clave("m1", "cronometro"));

        assertTrue(cronometros.activos().isEmpty());
        assertFalse(redis.opsForSet().isMember(CronometrosRedis.ACTIVOS, "m1"));
    }

    // ---------- reto ----------

    @Test
    void elRetoPasaPorSusEstadosUnaSolaVez() {
        RetosRedis retos = new RetosRedis(estado);
        Instant inicio = AHORA.plus(Duration.ofMinutes(9));

        assertTrue(retos.programar("m1", List.of("A", "B"), inicio));
        assertFalse(retos.programar("m1", List.of("X"), AHORA));
        assertEquals(new RetosPartida.Plan("m1", List.of("A", "B"), inicio, RetosPartida.Estado.PROGRAMADO),
                retos.plan("m1").orElseThrow());
        assertEquals(List.of("m1"), retos.activos());
        assertTrue(retos.enCurso("m1").isEmpty());

        Instant fin = AHORA.plusSeconds(20);
        assertTrue(retos.iniciar("m1", ROLES, fin));
        assertFalse(retos.iniciar("m1", ROLES, fin), "arranca una sola vez");
        assertEquals(new RetosPartida.EnCurso("m1", fin, Map.of("A", 0L, "B", 0L)), retos.enCurso("m1").orElseThrow());

        retos.click("m1", "ana", AHORA);  // heroe de A: +1 a A
        retos.click("m1", "dani", AHORA); // verdugo de B: -1 a cada rival (A)
        retos.click("m1", "caro", AHORA); // heroe de B: +1 a B
        assertEquals(Map.of("A", 0L, "B", 1L), retos.enCurso("m1").orElseThrow().puntajes());

        ChallengeResult resultado = retos.terminar("m1").orElseThrow();
        assertEquals("B", resultado.winnerTeamId());
        assertEquals(Map.of("A", 0L, "B", 1L), resultado.scores());
        assertTrue(retos.terminar("m1").isEmpty(), "se cierra una sola vez");
        assertThrows(ChallengeNotActiveException.class, () -> retos.click("m1", "ana", AHORA));
        assertTrue(retos.activos().isEmpty());
    }

    @Test
    void unClickFueraDeTiempoOAjenoSeRechaza() {
        RetosRedis retos = new RetosRedis(estado);
        retos.programar("m1", List.of("A", "B"), AHORA);
        assertThrows(ChallengeNotActiveException.class, () -> retos.click("m1", "ana", AHORA), "aun programado");
        retos.iniciar("m1", ROLES, AHORA.plusSeconds(20));

        assertThrows(NotInChallengeException.class, () -> retos.click("m1", "intruso", AHORA));
        Instant alTerminar = AHORA.plusSeconds(20);
        assertThrows(ChallengeNotActiveException.class, () -> retos.click("m1", "ana", alTerminar));
        assertThrows(ChallengeNotActiveException.class, () -> retos.click("otra", "ana", AHORA));
    }

    @Test
    void cancelarSoloAplicaAUnRetoProgramado() {
        RetosRedis retos = new RetosRedis(estado);
        retos.programar("m1", List.of("A", "B"), AHORA);

        retos.cancelar("m1");

        assertEquals(RetosPartida.Estado.CANCELADO, retos.plan("m1").orElseThrow().estado());
        assertFalse(retos.iniciar("m1", ROLES, AHORA.plusSeconds(20)));
        assertTrue(retos.activos().isEmpty());
        assertTrue(retos.plan("nunca").isEmpty());
    }

    /** Clicks por dos instancias a la vez y el cierre en medio: cada click contado es exacto, ninguno despues. */
    @Test
    void conClicksSimultaneosDesdeDosInstanciasElResultadoEsExacto() throws Exception {
        RetosRedis instanciaA = new RetosRedis(estado);
        RetosRedis instanciaB = new RetosRedis(estado);
        instanciaA.programar("m1", List.of("A", "B"), AHORA);
        instanciaA.iniciar("m1", ROLES, AHORA.plusSeconds(20));

        int clicks = 200;
        AtomicInteger contadosA = new AtomicInteger();
        ExecutorService hilos = Executors.newFixedThreadPool(8);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> tareas = new ArrayList<>();
        for (int i = 0; i < clicks; i++) {
            RetosRedis instancia = i % 2 == 0 ? instanciaA : instanciaB;
            tareas.add(hilos.submit(() -> {
                largada.await();
                try {
                    instancia.click("m1", "ana", AHORA);
                    contadosA.incrementAndGet();
                } catch (ChallengeNotActiveException e) {
                    // llego despues del cierre: no cuenta
                }
                return null;
            }));
        }
        largada.countDown();
        // Se cierra en plena carrera, con clicks ya contados
        await().atMost(Duration.ofSeconds(5)).until(() -> contadosA.get() > 0);
        ChallengeResult resultado = instanciaB.terminar("m1").orElseThrow();
        for (Future<?> t : tareas) t.get(30, TimeUnit.SECONDS);
        hilos.shutdown();

        assertEquals(contadosA.get(), resultado.scores().get("A"), "cada click aceptado esta en el resultado");
        assertEquals(contadosA.get(), Long.parseLong(String.valueOf(
                redis.opsForHash().get(RetosRedis.clave("m1") + ":puntajes", "A"))), "y ninguno entro despues");
    }

    // ---------- resultado ----------

    @Test
    void elResultadoSeGuardaUnaVezYVuelveIgual() {
        ResultadosRedis resultados = new ResultadosRedis(estado);
        resultados.guardarEquipos("m1", Map.of("B", List.of("caro"), "A", List.of("ana", "beto")));
        resultados.guardarEquipos("m1", Map.of("X", List.of("otro")));
        resultados.sumarRetoGanado("m1", "A");
        resultados.sumarRetoGanado("m1", "A");

        Map<String, List<String>> equipos = resultados.equipos("m1").orElseThrow();
        assertEquals(Map.of("A", List.of("ana", "beto"), "B", List.of("caro")), equipos);
        assertEquals(List.of("A", "B"), List.copyOf(equipos.keySet()));
        assertEquals(Map.of("A", 2), resultados.retosGanados("m1"));
        assertTrue(resultados.equipos("otra").isEmpty());
        assertTrue(resultados.retosGanados("otra").isEmpty());

        MatchResult empate = new MatchResult("m1", MatchResult.Reason.ALL_PUZZLES_SOLVED, null, List.of("A", "B"),
                List.of(new TeamScore("A", 3, 0, 3), new TeamScore("B", 2, 1, 3)));
        assertTrue(resultados.guardar(empate));
        assertFalse(resultados.guardar(new MatchResult("m1", MatchResult.Reason.TIME_UP, "B", List.of(), List.of())));

        MatchResult leido = resultados.find("m1").orElseThrow();
        assertEquals(empate, leido);
        assertTrue(leido.isDraw());
        assertTrue(resultados.find("otra").isEmpty());
    }

    @Test
    void alTerminarLaPartidaSusClavesDeTiempoTambienSeAcortan() {
        new CronometrosRedis(estado).crear(new MatchTimer("m1", AHORA, Duration.ofMinutes(5)));
        new ResultadosRedis(estado).guardarEquipos("m1", Map.of("A", List.of("ana"), "B", List.of("caro")));

        estado.acortar("m1", Duration.ofMinutes(5));

        assertTrue(segundosDeVida(EstadoPartidaRedis.clave("m1", "cronometro")) <= 300);
        assertTrue(segundosDeVida(EstadoPartidaRedis.clave("m1", "equipos")) <= 300);
    }
}
