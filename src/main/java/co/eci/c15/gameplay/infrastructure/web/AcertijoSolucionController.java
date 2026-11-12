package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ValidarSolucionUseCase;
import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.AcertijoNoAbiertoException;
import co.eci.c15.gameplay.domain.AcertijoYaResueltoException;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.SolucionNoDisponibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/partidas/{matchId}/acertijos/{componenteMapaId}/solucion")
public class AcertijoSolucionController {

    public record SolucionRequest(List<String> respuesta) {}

    private final ValidarSolucionUseCase useCase;

    public AcertijoSolucionController(ValidarSolucionUseCase useCase) {
        this.useCase = useCase;
    }

    /** Envia la solucion; sin "respuesta" se valida lo ingresado con clicks. Un intento incorrecto responde 200. */
    @PostMapping
    public ResponseEntity<Object> enviar(@PathVariable("matchId") String matchId,
                                    @PathVariable("componenteMapaId") String componenteMapaId,
                                    @RequestBody SolucionRequest body, Principal yo) {
        try {
            return ResponseEntity.ok(useCase.ejecutar(matchId, componenteMapaId, yo.getName(), body.respuesta()));
        } catch (JugadorNoRegistradoException | ComponenteSinAcertijoException | SolucionNoDisponibleException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AcertijoBloqueadoException | AcertijoNoAbiertoException | AcertijoYaResueltoException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
