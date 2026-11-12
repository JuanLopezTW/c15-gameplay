package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.RolesRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class RolesRepositoryEnMemoria implements RolesRepository {

    private final Map<String, Map<String, AsignacionRoles>> porPartida = new ConcurrentHashMap<>();

    @Override
    public AsignacionRoles saveIfAbsent(AsignacionRoles asignacion) {
        AsignacionRoles existente = porPartida
                .computeIfAbsent(asignacion.matchId(), id -> new ConcurrentHashMap<>())
                .putIfAbsent(asignacion.equipoId(), asignacion);
        return existente != null ? existente : asignacion;
    }

    @Override
    public Optional<AsignacionRoles> findByEquipo(String matchId, String equipoId) {
        Map<String, AsignacionRoles> partida = porPartida.get(matchId);
        return partida == null ? Optional.empty() : Optional.ofNullable(partida.get(equipoId));
    }

    @Override
    public Optional<Rol> findRol(String matchId, String userId) {
        Map<String, AsignacionRoles> partida = porPartida.get(matchId);
        if (partida == null) return Optional.empty();
        return partida.values().stream()
                .map(a -> a.roles().get(userId))
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }
}
