package co.eci.c15.gameplay.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A team's final score: {@code score = puzzlesSolved + challengesWon}. JSON in Spanish. */
public record TeamScore(@JsonProperty("equipoId") String teamId,
                        @JsonProperty("acertijosResueltos") int puzzlesSolved,
                        @JsonProperty("retosGanados") int challengesWon,
                        @JsonProperty("puntaje") int score) {
}
