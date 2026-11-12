package co.eci.c15.gameplay.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * How a match ended. {@code winnerTeamId} is null on a draw, and then {@code tiedTeamIds} lists
 * the teams that tied at the top. {@code standings} goes from first to last. The JSON goes out
 * in Spanish for the front.
 */
public record MatchResult(String matchId,
                          @JsonProperty("motivo") Reason reason,
                          @JsonProperty("equipoGanadorId") String winnerTeamId,
                          @JsonProperty("equiposEmpatados") List<String> tiedTeamIds,
                          @JsonProperty("clasificacion") List<TeamScore> standings) {

    public enum Reason {
        @JsonProperty("TIEMPO_AGOTADO") TIME_UP,
        @JsonProperty("ACERTIJOS_COMPLETOS") ALL_PUZZLES_SOLVED
    }

    public MatchResult {
        tiedTeamIds = List.copyOf(tiedTeamIds);
        standings = List.copyOf(standings);
    }

    @JsonProperty("empate")
    public boolean isDraw() {
        return winnerTeamId == null;
    }
}
