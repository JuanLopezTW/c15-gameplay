package co.eci.c15.gameplay.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class MatchTimer {

    private final String matchId;
    private final Instant startedAt;
    private final Duration duration;

    public MatchTimer(String matchId, Instant startedAt, Duration duration) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.duration = Objects.requireNonNull(duration, "duration");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("La duracion debe ser positiva");
        }
    }

    public Instant endsAt() {
        return startedAt.plus(duration);
    }

    public long remainingSeconds(Instant now) {
        long ms = Duration.between(now, endsAt()).toMillis();
        return ms <= 0 ? 0 : (ms + 999) / 1000;
    }

    public boolean isTimeUp(Instant now) {
        return !now.isBefore(endsAt());
    }

    public String getMatchId() { return matchId; }
    public Instant getStartedAt() { return startedAt; }
    public Duration getDuration() { return duration; }
}