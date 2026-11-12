package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComparadorRespuestasTest {

    private static final List<String> COLORES = List.of("rojo", "naranja", "añil");

    @Test
    void coincideConLaSolucionExacta() {
        assertTrue(ComparadorRespuestas.coincide(List.of("16"), List.of("16")));
        assertTrue(ComparadorRespuestas.coincide(List.of("rojo", "naranja", "añil"), COLORES));
    }

    @Test
    void ignoraMayusculasTildesYEspacios() {
        assertTrue(ComparadorRespuestas.coincide(List.of("  ESCAPE "), List.of("escape")));
        assertTrue(ComparadorRespuestas.coincide(List.of("Rojo", "NARANJA", "anil"), COLORES));
        assertTrue(ComparadorRespuestas.coincide(List.of("añil"), List.of("anil")));
    }

    @Test
    void aceptaLaRespuestaPartidaEnPiezas() {
        assertTrue(ComparadorRespuestas.coincide(List.of("E", "S", "C", "A", "P", "E"), List.of("escape")));
        assertTrue(ComparadorRespuestas.coincide(List.of("1", "6"), List.of("16")));
    }

    @Test
    void elOrdenImporta() {
        assertFalse(ComparadorRespuestas.coincide(List.of("naranja", "rojo", "añil"), COLORES));
        assertFalse(ComparadorRespuestas.coincide(List.of("6", "1"), List.of("16")));
    }

    @Test
    void respuestaIncompletaOConDeMasNoCoincide() {
        assertFalse(ComparadorRespuestas.coincide(List.of("rojo", "naranja"), COLORES));
        assertFalse(ComparadorRespuestas.coincide(List.of("rojo", "naranja", "añil", "verde"), COLORES));
        assertFalse(ComparadorRespuestas.coincide(List.of("17"), List.of("16")));
    }

    @Test
    void respuestaVaciaONulaNuncaCoincide() {
        assertFalse(ComparadorRespuestas.coincide(List.of(), List.of("16")));
        assertFalse(ComparadorRespuestas.coincide(null, List.of("16")));
        assertFalse(ComparadorRespuestas.coincide(List.of("16"), null));
        assertFalse(ComparadorRespuestas.coincide(List.of(" "), List.of("16")));
        assertFalse(ComparadorRespuestas.coincide(Arrays.asList((String) null), List.of("16")));
    }
}
