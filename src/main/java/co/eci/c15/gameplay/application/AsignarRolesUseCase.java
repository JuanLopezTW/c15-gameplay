package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.AsignadorRoles;
import co.eci.c15.gameplay.domain.RolesRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Asigna los roles de todos los equipos al iniciar la partida. Es idempotente: si un
 * equipo ya tiene roles se respetan y no se vuelve a avisar.
 */
@Service
public class AsignarRolesUseCase {

    private final AsignadorRoles asignador;
    private final RolesRepository roles;
    private final RolesNotifier notifier;

    public AsignarRolesUseCase(AsignadorRoles asignador, RolesRepository roles, RolesNotifier notifier) {
        this.asignador = asignador;
        this.roles = roles;
        this.notifier = notifier;
    }

    public List<AsignacionRolesDto> ejecutar(String matchId, Map<String, List<String>> equipos) {
        if (matchId == null || matchId.isBlank()) throw new IllegalArgumentException("El id de la partida no puede estar vacio");
        if (equipos == null || equipos.isEmpty()) throw new IllegalArgumentException("La partida no tiene equipos");
        List<AsignacionRolesDto> resultado = new ArrayList<>();
        new TreeMap<>(equipos).forEach((equipoId, miembros) -> {
            AsignacionRoles propuesta = new AsignacionRoles(matchId, equipoId, asignador.asignar(miembros));
            AsignacionRoles guardada = roles.saveIfAbsent(propuesta);
            AsignacionRolesDto dto = AsignacionRolesDto.from(guardada);
            if (guardada == propuesta) notifier.notificar(dto);
            resultado.add(dto);
        });
        return resultado;
    }
}
