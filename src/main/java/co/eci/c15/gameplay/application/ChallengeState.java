package co.eci.c15.gameplay.application;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * What players see of the challenge. {@code status}: INICIADO, EN_CURSO (live scores) or TERMINADO
 * (with {@code winnerTeamId}, null on a tie). The JSON goes out in Spanish for the front.
 */
public record ChallengeState(String matchId,
                             @JsonProperty("estado") String status,
                             @JsonProperty("segundosRestantes") long remainingSeconds,
                             @JsonProperty("puntajes") Map<String, Long> scores,
                             @JsonProperty("equipoGanadorId") String winnerTeamId) {

    public static final String STARTED = "INICIADO";
    public static final String RUNNING = "EN_CURSO";
    public static final String FINISHED = "TERMINADO";
}
