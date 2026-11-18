package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefinicionAcertijoTest {

    private static DefinicionAcertijo cooperativo() {
        return new DefinicionAcertijo("c", "cifrado", "Enunciado", List.of(),
                Map.of("nota", "1-1-1"),
                List.of(Map.of("paginas", List.of("p2"), "color", "rojo"), Map.of("paginas", List.of("p3"))),
                null, Map.of(), List.of(List.of("hola")));
    }

    @Test
    void cadaGrupoVeLosDatosComunesYSusPistas() {
        DefinicionAcertijo a = cooperativo();
        assertEquals(Map.of("nota", "1-1-1", "paginas", List.of("p2"), "color", "rojo"), a.datosPara(0, false, false));
        assertEquals(Map.of("nota", "1-1-1", "paginas", List.of("p3")), a.datosPara(1, false, false));
        // Si hay mas jugadores que grupos, se reparten en ronda
        assertEquals(a.datosPara(0, false, false), a.datosPara(2, false, false));
    }

    @Test
    void quienJuegaSoloRecibeTodasLasPistasJuntas() {
        assertEquals(List.of("p2", "p3"), cooperativo().datosPara(0, true, false).get("paginas"));
    }

    @Test
    void lasPistasRepartidasSeDetectan() {
        assertTrue(cooperativo().tienePistasRepartidas());
        DefinicionAcertijo simple = new DefinicionAcertijo("s", "pregunta", "E", List.of(), Map.of(), List.of(),
                null, Map.of(), List.of(List.of("x")));
        assertFalse(simple.tienePistasRepartidas());
        assertEquals(Map.of(), simple.datosPara(0, false, true));
    }

    @Test
    void lodelObjetoSoloSeMuestraSiElEquipoLoTiene() {
        DefinicionAcertijo uv = new DefinicionAcertijo("u", "uv", "E", List.of(), Map.of("cuadro", "Retrato"), List.of(),
                "lampara-uv", Map.of("oculto", "LUNA", "cuadro", "Retrato revelado"), List.of(List.of("luna")));
        assertEquals(Map.of("cuadro", "Retrato"), uv.datosPara(0, false, false));
        assertEquals(Map.of("cuadro", "Retrato revelado", "oculto", "LUNA"), uv.datosPara(0, false, true));
    }

    @Test
    void aceptaCualquieraDeSusRespuestas() {
        DefinicionAcertijo a = new DefinicionAcertijo("g", "globo", "E", List.of(), Map.of(), List.of(), null, Map.of(),
                List.of(List.of("Tokio"), List.of("Tokyo")));
        assertTrue(a.acepta(List.of("tokyo")));
        assertTrue(a.acepta(List.of("TOKIO")));
        assertFalse(a.acepta(List.of("Kioto")));
        assertFalse(a.acepta(null));
    }

    @Test
    void validaQueTengaSolucionYQueLoOcultoTengaObjeto() {
        List<ElementoInteractivo> sinElementos = List.of();
        Map<String, Object> vacio = Map.of();
        List<Map<String, Object>> sinPistas = List.of();
        List<List<String>> sinSoluciones = List.of();
        assertThrows(IllegalArgumentException.class, () -> new DefinicionAcertijo("x", "pregunta", "E", sinElementos,
                vacio, sinPistas, null, vacio, sinSoluciones));
        Map<String, Object> oculto = Map.of("oculto", "algo");
        List<List<String>> soluciones = List.of(List.of("a"));
        assertThrows(IllegalArgumentException.class, () -> new DefinicionAcertijo("x", "uv", "E", sinElementos,
                vacio, sinPistas, null, oculto, soluciones));
    }
}
