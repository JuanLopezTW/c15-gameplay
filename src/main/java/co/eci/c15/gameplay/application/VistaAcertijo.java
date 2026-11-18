package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.DefinicionAcertijo;
import co.eci.c15.gameplay.domain.InventarioRepository;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.RolesRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Arma lo que ve cada jugador de un acertijo: las pistas repartidas van por rol (grupo 0 el Heroe,
 * grupo 1 el Verdugo; quien juega solo en su equipo las ve todas) y lo oculto solo si el equipo ya
 * tiene el objeto que lo revela.
 */
@Component
public class VistaAcertijo {

    private final RolesRepository roles;
    private final InventarioRepository inventarios;

    public VistaAcertijo(RolesRepository roles, InventarioRepository inventarios) {
        this.roles = roles;
        this.inventarios = inventarios;
    }

    public VisualizarAcertijoDto para(String matchId, String equipoId, String userId, String componenteMapaId,
                                      DefinicionAcertijo acertijo, AcertijoEnPartida estado) {
        boolean tieneObjeto = acertijo.objetoRequerido() != null
                && inventarios.obtener(matchId, equipoId).getObjetos().stream()
                .anyMatch(o -> o.id().equals(acertijo.objetoRequerido()));
        int grupo = 0;
        boolean todas = false;
        if (acertijo.tienePistasRepartidas()) {
            Optional<AsignacionRoles> asignacion = roles.findByEquipo(matchId, equipoId);
            todas = asignacion.map(a -> a.roles().size() < 2).orElse(true);
            grupo = asignacion.map(a -> a.roles().get(userId)).map(r -> r == Rol.VERDUGO ? 1 : 0).orElse(0);
        }
        Map<String, Object> datos = acertijo.datosPara(grupo, todas, tieneObjeto);
        return VisualizarAcertijoDto.from(acertijo, componenteMapaId, estado, datos, tieneObjeto);
    }
}
