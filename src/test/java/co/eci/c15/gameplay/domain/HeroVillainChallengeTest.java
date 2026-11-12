package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class HeroVillainChallengeTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final Instant ENDS = NOW.plusSeconds(20);

    private HeroVillainChallenge challenge;

    @BeforeEach
    void setUp() {
        challenge = new HeroVillainChallenge("m1", Map.of(
                "A", Map.of("ana", Rol.HEROE, "beto", Rol.VERDUGO),
                "B", Map.of("caro", Rol.HEROE, "dani", Rol.VERDUGO)), ENDS);
    }

    @Test
    void heroClickAddsOneToTheirTeam() {
        challenge.click("ana", NOW);
        challenge.click("ana", NOW);
        assertEquals(Map.of("A", 2L, "B", 0L), challenge.scores());
    }

    @Test
    void executionerClickSubtractsOneFromTheRivalTeam() {
        challenge.click("beto", NOW);
        assertEquals(Map.of("A", 0L, "B", -1L), challenge.scores());
    }

    @Test
    void withThreeTeamsTheExecutionerHitsEveryRival() {
        HeroVillainChallenge three = new HeroVillainChallenge("m1", Map.of(
                "A", Map.of("ana", Rol.VERDUGO),
                "B", Map.of("caro", Rol.HEROE),
                "C", Map.of("eli", Rol.HEROE)), ENDS);
        three.click("ana", NOW);
        assertEquals(Map.of("A", 0L, "B", -1L, "C", -1L), three.scores());
    }

    @Test
    void playerOutsideTheChallengeCannotClick() {
        assertThrows(NotInChallengeException.class, () -> challenge.click("intruso", NOW));
    }

    @Test
    void clicksAfterTheTimeDoNotCount() {
        assertThrows(ChallengeNotActiveException.class, () -> challenge.click("ana", ENDS));
        assertEquals(0L, challenge.scores().get("A"));
    }

    @Test
    void clicksAfterFinishDoNotCount() {
        challenge.finish();
        assertTrue(challenge.isFinished());
        assertThrows(ChallengeNotActiveException.class, () -> challenge.click("ana", NOW));
    }

    @Test
    void theTeamWithMostPointsWins() {
        challenge.click("ana", NOW);
        challenge.click("ana", NOW);
        challenge.click("caro", NOW);

        ChallengeResult result = challenge.finish();

        assertEquals("A", result.winnerTeamId());
        assertEquals(Map.of("A", 2L, "B", 1L), result.scores());
    }

    @Test
    void negativeScoresStillPickTheHighest() {
        challenge.click("beto", NOW);
        challenge.click("dani", NOW);
        challenge.click("dani", NOW);

        ChallengeResult result = challenge.finish();

        assertEquals(Map.of("A", -2L, "B", -1L), result.scores());
        assertEquals("B", result.winnerTeamId());
    }

    @Test
    void aTieHasNoWinner() {
        challenge.click("ana", NOW);
        challenge.click("caro", NOW);
        assertTrue(challenge.finish().winner().isEmpty());
    }

    @Test
    void nobodyClickingIsATie() {
        assertNull(challenge.finish().winnerTeamId());
    }

    @Test
    void needsAtLeastTwoTeams() {
        assertThrows(IllegalArgumentException.class,
                () -> new HeroVillainChallenge("m1", Map.of("A", Map.of("ana", Rol.HEROE)), ENDS));
    }

    @Test
    void manyConcurrentClicksAreCountedExactly() throws Exception {
        int clicksPerThread = 5_000;
        Map<String, Integer> threadsPerPlayer = Map.of("ana", 6, "beto", 2, "caro", 4, "dani", 3);
        ExecutorService pool = Executors.newFixedThreadPool(15);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        threadsPerPlayer.forEach((player, threads) -> {
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    for (int i = 0; i < clicksPerThread; i++) challenge.click(player, NOW);
                    return null;
                }));
            }
        });
        go.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        // A = ana (hero of A) - dani (executioner of B); B = caro (hero of B) - beto (executioner of A)
        long a = (6L - 3L) * clicksPerThread;
        long b = (4L - 2L) * clicksPerThread;
        ChallengeResult result = challenge.finish();
        assertEquals(Map.of("A", a, "B", b), result.scores());
        assertEquals("A", result.winnerTeamId());
    }

    @Test
    void finishingWhileClicksArriveCountsExactlyTheAcceptedOnes() throws Exception {
        HeroVillainChallenge race = new HeroVillainChallenge("m1", Map.of(
                "A", Map.of("ana", Rol.HEROE),
                "B", Map.of("caro", Rol.HEROE)), ENDS);
        AtomicLong acceptedA = new AtomicLong();
        AtomicLong acceptedB = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            String player = t % 2 == 0 ? "ana" : "caro";
            AtomicLong accepted = t % 2 == 0 ? acceptedA : acceptedB;
            futures.add(pool.submit(() -> {
                go.await();
                while (true) {
                    try {
                        race.click(player, NOW);
                        accepted.incrementAndGet();
                    } catch (ChallengeNotActiveException closed) {
                        return null;
                    }
                }
            }));
        }
        go.countDown();
        Thread.sleep(50);
        ChallengeResult result = race.finish();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(acceptedA.get() + acceptedB.get() > 0);
        assertEquals(acceptedA.get(), result.scores().get("A"));
        assertEquals(acceptedB.get(), result.scores().get("B"));
        assertEquals(result.scores(), race.scores());
    }
}
