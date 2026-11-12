package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MatchTimerTest {

    private final Instant t0 = Instant.parse("2026-10-05T15:00:00Z");
    private final MatchTimer timer = new MatchTimer("m1", t0, Duration.ofSeconds(60));

    @Test
    void showsFullDurationAtStart() {
        assertEquals(60, timer.remainingSeconds(t0));
        assertFalse(timer.isTimeUp(t0));
    }

    @Test
    void roundsUp() {
        assertEquals(60, timer.remainingSeconds(t0.plusMillis(500)));
        assertEquals(59, timer.remainingSeconds(t0.plusSeconds(1)));
    }

    @Test
    void reachesZeroAndIsTimeUp() {
        assertEquals(0, timer.remainingSeconds(t0.plusSeconds(60)));
        assertTrue(timer.isTimeUp(t0.plusSeconds(60)));
        assertEquals(0, timer.remainingSeconds(t0.plusSeconds(90)));
    }

    @Test
    void rejectsInvalidDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchTimer("m1", t0, Duration.ZERO));
    }
}