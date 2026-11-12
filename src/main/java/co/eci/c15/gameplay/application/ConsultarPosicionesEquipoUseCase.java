package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** Posiciones actuales de un equipo, visibles solo para sus propios miembros. */
@Service
public class ConsultarPosicionesEquipoUseCase {

    private final JugadorEnMapaRepository jugadores;

    public ConsultarPosicionesEquipoUseCase(JugadorEnMapaRepository jugadores) {
        this.jugadores = jugadores;
    }

    public List<PosicionJugadorDto> ejecutar(String matchId, String equipoId, String solicitanteId) {
        JugadorEnMapa solicitante = jugadores.find(matchId, solicitanteId)
                .orElseThrow(() -> new JugadorNoRegistradoException(matchId, solicitanteId));
        if (!solicitante.getEquipoId().equals(equipoId)) {
            throw new AccesoEquipoDenegadoException(solicitanteId, equipoId);
        }
        return jugadores.findByMatchId(matchId).stream()
                .filter(j -> j.getEquipoId().equals(equipoId))
                .sorted(Comparator.comparing(JugadorEnMapa::getUserId))
                .map(j -> PosicionJugadorDto.conectado(j.getUserId(), j.getPosicion()))
                .toList();
    }
}
