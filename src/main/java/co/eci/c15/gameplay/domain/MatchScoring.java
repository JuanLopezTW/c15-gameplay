package co.eci.c15.gameplay.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Final score of a match (HU-67.6): one point per solved puzzle plus one per challenge won.
 * The same score is a draw (no winner), even if the teams solved a different number of puzzles:
 * winning the challenge is worth exactly one puzzle (Juan, 2026-10-12). Puzzles only order
 * tied teams in the standings.
 */
public final class MatchScoring {

    private static final Comparator<TeamScore> RANKING = Comparator
            .comparingInt(TeamScore::score).reversed()
            .thenComparing(Comparator.comparingInt(TeamScore::puzzlesSolved).reversed())
            .thenComparing(TeamScore::teamId);

    private MatchScoring() {}

    public static MatchResult rank(String matchId, MatchResult.Reason reason, Collection<String> teamIds,
                                   Map<String, Integer> puzzlesSolved, Map<String, Integer> challengesWon) {
        List<TeamScore> standings = new ArrayList<>();
        for (String team : teamIds) {
            int puzzles = puzzlesSolved.getOrDefault(team, 0);
            int challenges = challengesWon.getOrDefault(team, 0);
            standings.add(new TeamScore(team, puzzles, challenges, puzzles + challenges));
        }
        standings.sort(RANKING);
        if (standings.isEmpty()) throw new IllegalArgumentException("La partida no tiene equipos");

        TeamScore top = standings.get(0);
        List<String> leaders = standings.stream()
                .filter(t -> t.score() == top.score())
                .map(TeamScore::teamId)
                .toList();
        String winner = leaders.size() == 1 ? top.teamId() : null;
        return new MatchResult(matchId, reason, winner, leaders.size() > 1 ? leaders : List.of(), standings);
    }
}
