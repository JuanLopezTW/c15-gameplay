package co.eci.c15.gameplay.infrastructure.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prueba de diagnostico (escenario O1): una peticion que falla a proposito con un 500. El jugador veria el codigo
 * (los primeros caracteres del traceId, en X-Trace-Id) y con ese codigo el admin encuentra la replica que la
 * atendio y la excepcion. No cambia nada del juego; como todo /dev, solo la puede usar el admin.
 */
@RestController
@RequestMapping("/dev/partidas")
public class DevErrorSimuladoController {

    @PostMapping("/{matchId}/error")
    public void fallar(@PathVariable("matchId") String matchId) {
        throw new ErrorSimuladoException(matchId);
    }

    /** La excepcion que se busca despues por el codigo. */
    public static class ErrorSimuladoException extends IllegalStateException {
        public ErrorSimuladoException(String matchId) {
            super("Error simulado para la prueba de diagnostico (O1) en la partida " + matchId);
        }
    }
}
