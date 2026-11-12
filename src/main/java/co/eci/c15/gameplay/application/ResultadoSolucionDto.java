package co.eci.c15.gameplay.application;

/** Resultado de un intento de solucion: si fue correcto y si el acertijo quedo resuelto. */
public record ResultadoSolucionDto(boolean correcto, boolean resuelto, String mensaje) {

    static ResultadoSolucionDto acierto() {
        return new ResultadoSolucionDto(true, true, "¡Correcto! El acertijo quedó resuelto");
    }

    static ResultadoSolucionDto error() {
        return new ResultadoSolucionDto(false, false, "Incorrecto, vuelve a intentarlo");
    }
}
