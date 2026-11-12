package co.eci.c15.gameplay.domain;

import java.util.Optional;

public interface RolesRepository {

    /** Guarda la asignacion solo si el equipo aun no tiene una; devuelve la que quedo guardada. */
    AsignacionRoles saveIfAbsent(AsignacionRoles asignacion);

    Optional<AsignacionRoles> findByEquipo(String matchId, String equipoId);

    Optional<Rol> findRol(String matchId, String userId);
}
