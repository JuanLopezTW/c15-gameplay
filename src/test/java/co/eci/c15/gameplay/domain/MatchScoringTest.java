package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MatchScoringTest {

    private static final List<String> TEAMS = List.of("A", "B");

    private static MatchResult rank(Map<String, Integer> puzzles, Map<String, Integer> challenges) {
        return MatchScoring.rank("m1", MatchResult.Reason.TIME_UP, TEAMS, puzzles, challenges);
    }

    @Test
    void scoreIsPuzzlesPlusChallengesWon() {
        MatchResult result = rank(Map.of("A", 2, "B", 1), Map.of("B", 1));

        assertEquals(List.of(new TeamScore("A", 2, 0, 2), new TeamScore("B", 1, 1, 2)), result.standings());
    }

    @Test
    void mostPointsWins() {
        MatchResult result = rank(Map.of("A", 1, "B", 3), Map.of());
        assertEquals("B", result.winnerTeamId());
        assertFalse(result.isDraw());
        assertTrue(result.tiedTeamIds().isEmpty());
    }

    @Test
    void theChallengeCountsToWin() {
        MatchResult result = rank(Map.of("A", 2, "B", 2), Map.of("A", 1));
        assertEquals("A", result.winnerTeamId());
    }

    @Test
    void challengeAgainstAPuzzleIsADraw() {
        // A wins the challenge (1), B solves one puzzle (1): same score -> draw
        MatchResult result = rank(Map.of("B", 1), Map.of("A", 1));
        assertTrue(result.isDraw());
        assertNull(result.winnerTeamId());
        assertEquals(List.of("B", "A"), result.tiedTeamIds());
    }

    @Test
    void sameScoreIsADrawEvenWithDifferentPuzzles() {
        // A: 2 puzzles = 2; B: 1 puzzle + challenge = 2 -> draw; A goes first in the standings
        MatchResult result = rank(Map.of("A", 2, "B", 1), Map.of("B", 1));
        assertTrue(result.isDraw());
        assertEquals(List.of("A", "B"), result.tiedTeamIds());
        assertEquals("A", result.standings().get(0).teamId());
    }

    @Test
    void allPuzzlesAgainstPuzzlesPlusChallengeIsADraw() {
        // A solves all 3 (3); B solves 2 and wins the challenge (3): same score -> draw
        MatchResult result = MatchScoring.rank("m1", MatchResult.Reason.ALL_PUZZLES_SOLVED, TEAMS,
                Map.of("A", 3, "B", 2), Map.of("B", 1));
        assertTrue(result.isDraw());
    }

    @Test
    void sameScoreAndSamePuzzlesIsADraw() {
        MatchResult result = rank(Map.of("A", 2, "B", 2), Map.of());
        assertTrue(result.isDraw());
        assertNull(result.winnerTeamId());
        assertEquals(List.of("A", "B"), result.tiedTeamIds());
    }

    @Test
    void teamsWithoutProgressHaveZero() {
        MatchResult result = rank(Map.of(), Map.of());
        assertTrue(result.isDraw());
        assertEquals(0, result.standings().get(0).score());
    }

    @Test
    void drawOnlyBetweenTheLeaders() {
        MatchResult result = MatchScoring.rank("m1", MatchResult.Reason.TIME_UP, List.of("A", "B", "C"),
                Map.of("A", 2, "B", 2, "C", 0), Map.of());
        assertEquals(List.of("A", "B"), result.tiedTeamIds());
        assertEquals("C", result.standings().get(2).teamId());
    }

    @Test
    void needsTeams() {
        assertThrows(IllegalArgumentException.class,
                () -> MatchScoring.rank("m1", MatchResult.Reason.TIME_UP, List.of(), Map.of(), Map.of()));
    }
}
