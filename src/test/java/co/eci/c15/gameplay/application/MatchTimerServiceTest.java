package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.events.TimeUpEvent;
import co.eci.c15.gameplay.infrastructure.estado.TiempoPartidaEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchTimerServiceTest {

    /** Reloj que se avanza a mano: el test no espera segundos reales. */
    static class MutableClock extends Clock {
        private volatile Instant now;
        MutableClock(Instant start) { this.now = start; }
        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private MutableClock clock;
    private CronometrosPartida cronometros;
    private Set<String> mias;
    private List<TimerState> sent;
    private List<Object> events;
    private MatchTimerService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-05T15:00:00Z"));
        cronometros = new TiempoPartidaEnMemoria().cronometrosEnMemoria();
        mias = ConcurrentHashMap.newKeySet();
        mias.add("m1");
        sent = new CopyOnWriteArrayList<>();
        events = new CopyOnWriteArrayList<>();
        service = new MatchTimerService(cronometros, mias::contains, clock, sent::add, events::add);
    }

    @Test
    void startingTwiceDoesNotDuplicateTheTimer() {
        var first = service.start("m1", Duration.ofSeconds(60));
        var second = service.start("m1", Duration.ofSeconds(60));
        assertSame(first, second);
        assertEquals(1, sent.size(), "solo el primero avisa");
        assertEquals(List.of("m1"), cronometros.activos());
    }

    @Test
    void stopRemovesTheTimerWithoutTimeUp() {
        service.start("m1", Duration.ofSeconds(60));

        assertTrue(service.stop("m1"));

        assertTrue(service.find("m1").isEmpty());
        clock.advance(Duration.ofSeconds(61));
        service.tickTodas();
        assertTrue(events.isEmpty());
        assertFalse(service.stop("m1"));
    }

    @Test
    void findReturnsRemainingTime() {
        service.start("m1", Duration.ofSeconds(60));
        clock.advance(Duration.ofSeconds(10));
        assertEquals(50, service.find("m1").orElseThrow().remainingSeconds());
    }

    @Test
    void eachTickAnnouncesTheRemainingTime() {
        service.start("m1", Duration.ofSeconds(60));
        clock.advance(Duration.ofSeconds(1));

        service.tickTodas();

        assertEquals(59, sent.get(sent.size() - 1).remainingSeconds());
        assertFalse(sent.get(sent.size() - 1).timeUp());
    }

    @Test
    void timeUpEventIsPublishedOnlyOnce() {
        service.start("m1", Duration.ofSeconds(5));
        clock.advance(Duration.ofSeconds(5));
        service.tick("m1");
        service.tick("m1");
        assertEquals(1, events.stream().filter(TimeUpEvent.class::isInstance).count());
        assertTrue(service.find("m1").isEmpty());
    }

    @Test
    void concurrentStartsCreateASingleTimer() throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                go.await();
                return service.start("m1", Duration.ofSeconds(60));
            });
        }
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, sent.size());
        assertEquals(1, cronometros.activos().size());
    }

    @Test
    void onlyTheMatchesThisInstanceOwnsAreTicked() {
        service.start("m1", Duration.ofSeconds(5));
        service.start("m2", Duration.ofSeconds(5));
        sent.clear();
        clock.advance(Duration.ofSeconds(5));

        service.tickTodas();

        assertEquals(List.of("m1"), sent.stream().map(TimerState::matchId).toList());
        assertEquals(List.of(new TimeUpEvent("m1", clock.instant())), events);
        assertTrue(service.find("m2").isPresent(), "la otra partida la avanza su dueño");
    }

    @Test
    void anErrorDoesNotStopTheClock() {
        CronometrosPartida roto = mock(CronometrosPartida.class);
        when(roto.activos()).thenThrow(new IllegalStateException("sin Redis"));
        MatchTimerService conError = new MatchTimerService(roto, m -> true, clock, sent::add, events::add);
        assertDoesNotThrow(conError::tickTodas);

        service.start("m1", Duration.ofSeconds(60));
        MatchTimerService fallaUna = new MatchTimerService(cronometros, m -> {
            throw new IllegalStateException("una partida falla");
        }, clock, sent::add, events::add);
        assertDoesNotThrow(fallaUna::tickTodas);
    }
}
