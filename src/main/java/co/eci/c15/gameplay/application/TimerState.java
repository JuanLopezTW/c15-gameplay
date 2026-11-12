package co.eci.c15.gameplay.application;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Remaining time of a match. The JSON goes out in Spanish for the front. */
public record TimerState(String matchId,
                         @JsonProperty("segundosRestantes") long remainingSeconds,
                         @JsonProperty("tiempoAgotado") boolean timeUp) {
}
