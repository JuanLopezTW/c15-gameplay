package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.InventarioRepository;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import org.springframework.stereotype.Service;

/** Inventario actual de un equipo, visible solo para sus propios miembros. */
@Service
public class ConsultarInventarioUseCase {

    private final JugadorEnMapaRepository jugadores;
    private final InventarioRepository inventarios;

    public ConsultarInventarioUseCase(JugadorEnMapaRepository jugadores, InventarioRepository inventarios) {
        this.jugadores = jugadores;
        this.inventarios = inventarios;
    }

    public InventarioDto ejecutar(String matchId, String equipoId, String solicitanteId) {
        JugadorEnMapa solicitante = jugadores.find(matchId, solicitanteId)
                .orElseThrow(() -> new JugadorNoRegistradoException(matchId, solicitanteId));
        if (!solicitante.getEquipoId().equals(equipoId)) {
            throw new AccesoEquipoDenegadoException(solicitanteId, equipoId);
        }
        return InventarioDto.from(inventarios.obtener(matchId, equipoId));
    }
}
