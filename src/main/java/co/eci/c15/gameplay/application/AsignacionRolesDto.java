package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.Rol;

import java.util.Map;
import java.util.TreeMap;

public record AsignacionRolesDto(String matchId, String equipoId, Map<String, Rol> roles) {

    public static AsignacionRolesDto from(AsignacionRoles asignacion) {
        return new AsignacionRolesDto(asignacion.matchId(), asignacion.equipoId(), new TreeMap<>(asignacion.roles()));
    }
}
