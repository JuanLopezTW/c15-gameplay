package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.events.ChallengeFinishedEvent;
import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.ChallengeNotActiveException;
import co.eci.c15.gameplay.domain.ChallengeResult;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.RolesRepository;
import co.eci.c15.gameplay.infrastructure.estado.TiempoPartidaEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.RolesRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HeroVillainChallengeServiceTest {

    private static final Duration PARTIDA = Duration.ofMinutes(30);

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    /** Con el azar en 0 el reto cae al 30% de 30 min. */
    private static final Duration A_LOS_9_MIN = Duration.ofMinutes(9);

    private MatchTimerServiceTest.MutableClock clock;
    private RetosPartida retos;
    private Set<String> mias;
    private RolesRepository roles;
    private ChallengeNotifier notifier;
    private ApplicationEventPublisher events;
    private HeroVillainChallengeService service;

    @BeforeEach
    void setUp() {
        clock = new MatchTimerServiceTest.MutableClock(NOW);
        retos = new TiempoPartidaEnMemoria().retosEnMemoria();
        mias = ConcurrentHashMap.newKeySet();
        mias.addAll(List.of("m1", "m2", "m3"));
        roles = new RolesRepositoryEnMemoria();
        notifier = mock(ChallengeNotifier.class);
        events = mock(ApplicationEventPublisher.class);
        RandomGenerator lowest = new RandomGenerator() {
            @Override public long nextLong() { return 0; }
            @Override public long nextLong(long bound) { return 0; }
        };
        service = new HeroVillainChallengeService(retos, mias::contains, clock, roles, notifier,
                events, lowest, Duration.ofSeconds(20));
        roles.saveIfAbsent(new AsignacionRoles("m1", "A", Map.of("ana", Rol.HEROE, "beto", Rol.VERDUGO)));
        roles.saveIfAbsent(new AsignacionRoles("m1", "B", Map.of("caro", Rol.HEROE, "dani", Rol.VERDUGO)));
    }

    private ChallengeState ultimoAviso(int avisos) {
        ArgumentCaptor<ChallengeState> state = ArgumentCaptor.forClass(ChallengeState.class);
        verify(notifier, times(avisos)).notify(state.capture());
        return state.getValue();
    }

    @Test
    void scheduleProgramsTheChallengeInsideTheWindowOnlyOnce() {
        service.schedule("m1", List.of("B", "A"), PARTIDA);
        service.schedule("m1", List.of("A", "B", "C"), PARTIDA);

        RetosPartida.Plan plan = retos.plan("m1").orElseThrow();
        assertEquals(NOW.plus(A_LOS_9_MIN), plan.inicio());
        assertEquals(List.of("A", "B"), plan.equipos());
        assertFalse(service.isActive("m1"));
    }

    @Test
    void beforeItsMomentTheTickDoesNotStartIt() {
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        clock.advance(A_LOS_9_MIN.minusSeconds(1));

        service.tickTodos();

        assertFalse(service.isActive("m1"));
        verifyNoInteractions(notifier);
    }

    @Test
    void whenTheMomentArrivesTheChallengeStarts() {
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        clock.advance(A_LOS_9_MIN);

        service.tickTodos();

        assertTrue(service.isActive("m1"));
        ChallengeState state = ultimoAviso(1);
        assertEquals(ChallengeState.STARTED, state.status());
        assertEquals(20, state.remainingSeconds());
        assertEquals(Map.of("A", 0L, "B", 0L), state.scores());
    }

    @Test
    void whileRunningTheTickBroadcastsAndWhenTimeIsUpItCloses() {
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        service.start("m1");
        service.click("m1", "ana");
        clock.advance(Duration.ofSeconds(5));

        service.tickTodos();
        ChallengeState enCurso = ultimoAviso(2);
        assertEquals(ChallengeState.RUNNING, enCurso.status());
        assertEquals(15, enCurso.remainingSeconds());
        assertEquals(Map.of("A", 1L, "B", 0L), enCurso.scores());

        clock.advance(Duration.ofSeconds(15));
        service.tickTodos();

        assertFalse(service.isActive("m1"));
        verify(events).publishEvent(new ChallengeFinishedEvent("m1", "A", Map.of("A", 1L, "B", 0L)));
        assertTrue(retos.activos().isEmpty());
    }

    @Test
    void onlyTheMatchesThisInstanceOwnsAreTicked() {
        mias.remove("m1");
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        clock.advance(A_LOS_9_MIN);

        service.tickTodos();

        assertFalse(service.isActive("m1"), "lo arranca su dueño");
    }

    @Test
    void clicksGoToTheRunningChallenge() {
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        service.start("m1");

        service.click("m1", "ana");
        service.click("m1", "dani");

        assertEquals(Map.of("A", 0L, "B", 0L), service.find("m1").orElseThrow().scores());
    }

    @Test
    void clickWithoutChallengeIsRejected() {
        assertThrows(ChallengeNotActiveException.class, () -> service.click("m1", "ana"));
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        assertThrows(ChallengeNotActiveException.class, () -> service.click("m1", "ana"), "programado aun no cuenta");
    }

    @Test
    void finishPublishesTheWinnerAndUnfreezesTheMap() {
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        service.start("m1");
        service.click("m1", "caro");

        ChallengeResult result = service.finish("m1").orElseThrow();

        assertEquals("B", result.winnerTeamId());
        assertFalse(service.isActive("m1"));
        verify(events).publishEvent(new ChallengeFinishedEvent("m1", "B", Map.of("A", 0L, "B", 1L)));
        ChallengeState state = ultimoAviso(2);
        assertEquals(ChallengeState.FINISHED, state.status());
        assertEquals("B", state.winnerTeamId());
        assertTrue(service.finish("m1").isEmpty(), "se cierra una sola vez");
    }

    @Test
    void aTiePublishesNoWinner() {
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        service.start("m1");

        service.finish("m1");

        verify(events).publishEvent(new ChallengeFinishedEvent("m1", null, Map.of("A", 0L, "B", 0L)));
    }

    @Test
    void onlyOneChallengePerMatch() {
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        assertTrue(service.start("m1"));
        service.finish("m1");

        assertFalse(service.start("m1"));
        assertFalse(service.isActive("m1"));
    }

    @Test
    void startingEarlyMeansTheScheduledMomentDoesNothing() {
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        service.start("m1");
        service.finish("m1");
        clock.advance(A_LOS_9_MIN);

        service.tickTodos();

        assertFalse(service.isActive("m1"));
        verify(events, times(1)).publishEvent(any(ChallengeFinishedEvent.class));
    }

    @Test
    void unknownMatchDoesNotStart() {
        assertFalse(service.start("no-existe"));
    }

    @Test
    void withoutRolesForTwoTeamsThereIsNoChallenge() {
        service.schedule("m2", List.of("X", "Y"), PARTIDA);
        assertFalse(service.start("m2"));
        assertFalse(service.isActive("m2"));
        verifyNoInteractions(notifier);
        assertTrue(retos.activos().isEmpty(), "queda cancelado");
    }

    @Test
    void whenTheMatchEndsTheChallengeIsDroppedOrClosed() {
        service.schedule("m1", List.of("A", "B"), PARTIDA);
        service.matchOver("m1");
        assertFalse(service.start("m1"));
        verify(events, never()).publishEvent(any());

        service.schedule("m3", List.of("A", "B"), PARTIDA);
        roles.saveIfAbsent(new AsignacionRoles("m3", "A", Map.of("ana", Rol.HEROE)));
        roles.saveIfAbsent(new AsignacionRoles("m3", "B", Map.of("caro", Rol.HEROE)));
        service.start("m3");
        service.matchOver("m3");
        assertFalse(service.isActive("m3"));
        verify(events).publishEvent(any(ChallengeFinishedEvent.class));
    }

    @Test
    void anErrorDoesNotStopTheClock() {
        RetosPartida roto = mock(RetosPartida.class);
        when(roto.activos()).thenThrow(new IllegalStateException("sin Redis"));
        HeroVillainChallengeService conError = new HeroVillainChallengeService(roto, m -> true, clock, roles,
                notifier, events, RandomGenerator.getDefault(), Duration.ofSeconds(20));
        assertDoesNotThrow(conError::tickTodos);

        doReturn(List.of("m1")).when(roto).activos();
        doThrow(new IllegalStateException("una partida falla")).when(roto).plan("m1");
        assertDoesNotThrow(conError::tickTodos);
    }
}
