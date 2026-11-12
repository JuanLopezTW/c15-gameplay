package co.eci.c15.gameplay.domain;

import java.time.Duration;
import java.util.random.RandomGenerator;

/** When the challenge fires: a random moment between 30% and 70% of the match (HU-44.5). */
public final class ChallengeTiming {

    public static final double WINDOW_START = 0.30;
    public static final double WINDOW_END = 0.70;

    private ChallengeTiming() {}

    public static Duration randomStart(Duration matchDuration, RandomGenerator random) {
        long total = matchDuration.toMillis();
        long from = Math.round(total * WINDOW_START);
        long to = Math.round(total * WINDOW_END);
        return Duration.ofMillis(from + random.nextLong(to - from + 1));
    }
}
