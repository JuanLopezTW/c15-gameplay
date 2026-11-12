package co.eci.c15.gameplay.domain;

import java.util.Map;
import java.util.Objects;

/** Roles de los miembros de un equipo en una partida. Inmutable. */
public record AsignacionRoles(String matchId, String equipoId, Map<String, Rol> roles) {

    public AsignacionRoles {
        Objects.requireNonNull(matchId);
        Objects.requireNonNull(equipoId);
        roles = Map.copyOf(roles);
    }
}
