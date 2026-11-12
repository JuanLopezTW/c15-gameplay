package co.eci.c15.gameplay.domain.events;

import java.util.Map;

/**
 * The Hero vs Executioner challenge ended (HU-44.5). {@code winnerTeamId} is null on a tie;
 * the winner gets the same reward as solving one puzzle. {@code scores} are the click points.
 */
public record ChallengeFinishedEvent(String matchId, String winnerTeamId, Map<String, Long> scores) {
}
