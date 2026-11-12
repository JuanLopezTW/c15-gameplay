package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeTimingTest {

    private static final Duration MATCH = Duration.ofMinutes(30);

    /** Random that always returns the given offset from the lower bound. */
    private static RandomGenerator fixed(boolean max) {
        return new RandomGenerator() {
            @Override public long nextLong() { return 0; }
            @Override public long nextLong(long bound) { return max ? bound - 1 : 0; }
        };
    }

    @Test
    void earliestMomentIsThirtyPercent() {
        assertEquals(Duration.ofMinutes(9), ChallengeTiming.randomStart(MATCH, fixed(false)));
    }

    @Test
    void latestMomentIsSeventyPercent() {
        assertEquals(Duration.ofMinutes(21), ChallengeTiming.randomStart(MATCH, fixed(true)));
    }

    @Test
    void alwaysInsideTheWindow() {
        RandomGenerator random = new SplittableRandom(42);
        for (int i = 0; i < 1_000; i++) {
            Duration start = ChallengeTiming.randomStart(MATCH, random);
            assertTrue(start.compareTo(Duration.ofMinutes(9)) >= 0, start::toString);
            assertTrue(start.compareTo(Duration.ofMinutes(21)) <= 0, start::toString);
        }
    }
}
