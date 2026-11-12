package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.PartidaOcupadaException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Si el candado del equipo no se pudo tomar a tiempo: 503 con Retry-After, el cliente puede reintentar. */
@RestControllerAdvice
public class PartidaOcupadaHandler {

    @ExceptionHandler(PartidaOcupadaException.class)
    public ResponseEntity<Map<String, String>> ocupada(PartidaOcupadaException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(Map.of("error", e.getMessage()));
    }
}
