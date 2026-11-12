package co.eci.c15.gameplay.domain.events;

import java.time.Instant;


public record TimeUpEvent(String matchId, Instant occurredAt) {
}