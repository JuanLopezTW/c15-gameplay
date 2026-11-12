package co.eci.c15.gameplay.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Click race between teams (HU-44.5). Each Hero click adds 1 to their own team and each
 * Executioner (Verdugo) click subtracts 1 from every rival team.
 *
 * <p>Concurrency: clicks share the read lock, so any number of them run in parallel and are
 * counted with {@link LongAdder} (no contention). {@link #finish} takes the write lock: it
 * waits for the clicks in flight and, once it returns, no other click is counted. That makes
 * the final result exact no matter how many clicks arrive at the same time.
 */
public final class HeroVillainChallenge {

    private final String matchId;
    private final Instant endsAt;
    private final Map<String, String> teamOf = new HashMap<>();
    private final Map<String, Rol> roleOf = new HashMap<>();
    private final Map<String, LongAdder> scores;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean finished;

    /** @param rolesByTeam team id → (user id → role) of every team that plays the challenge */
    public HeroVillainChallenge(String matchId, Map<String, Map<String, Rol>> rolesByTeam, Instant endsAt) {
        this.matchId = Objects.requireNonNull(matchId);
        this.endsAt = Objects.requireNonNull(endsAt);
        if (rolesByTeam.size() < 2) throw new IllegalArgumentException("El reto necesita al menos dos equipos");
        Map<String, LongAdder> byTeam = new LinkedHashMap<>();
        new TreeMap<>(rolesByTeam).forEach((team, roles) -> {
            byTeam.put(team, new LongAdder());
            roles.forEach((user, role) -> {
                teamOf.put(user, team);
                roleOf.put(user, role);
            });
        });
        this.scores = Collections.unmodifiableMap(byTeam);
    }

    public void click(String userId, Instant now) {
        String team = teamOf.get(userId);
        if (team == null) throw new NotInChallengeException(matchId, userId);
        lock.readLock().lock();
        try {
            if (finished || !now.isBefore(endsAt)) throw new ChallengeNotActiveException(matchId);
            if (roleOf.get(userId) == Rol.HEROE) {
                scores.get(team).increment();
            } else {
                scores.forEach((rival, score) -> {
                    if (!rival.equals(team)) score.decrement();
                });
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Current scores by team (ordered by team id). While running it is a live, approximate view. */
    public Map<String, Long> scores() {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        scores.forEach((team, score) -> snapshot.put(team, score.sum()));
        return snapshot;
    }

    /** Closes the challenge. The winner is the team with the most points; a tie at the top has no winner. */
    public ChallengeResult finish() {
        lock.writeLock().lock();
        try {
            finished = true;
            return ChallengeResult.deMarcador(matchId, scores());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isFinished() {
        lock.readLock().lock();
        try {
            return finished;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getMatchId() { return matchId; }
    public Instant getEndsAt() { return endsAt; }
}
