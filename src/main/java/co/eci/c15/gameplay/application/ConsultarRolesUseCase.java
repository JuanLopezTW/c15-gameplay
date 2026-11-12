package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.RolNoAsignadoException;
import co.eci.c15.gameplay.domain.RolesRepository;
import org.springframework.stereotype.Service;

@Service
public class ConsultarRolesUseCase {

    public record RolDto(String userId, Rol rol) {}

    private final RolesRepository roles;

    public ConsultarRolesUseCase(RolesRepository roles) {
        this.roles = roles;
    }

    public RolDto rolDe(String matchId, String userId) {
        Rol rol = roles.findRol(matchId, userId).orElseThrow(() -> new RolNoAsignadoException(matchId, userId));
        return new RolDto(userId, rol);
    }

    public AsignacionRolesDto rolesDelEquipo(String matchId, String equipoId) {
        return roles.findByEquipo(matchId, equipoId)
                .map(AsignacionRolesDto::from)
                .orElseThrow(() -> new RolNoAsignadoException(matchId, equipoId));
    }
}
