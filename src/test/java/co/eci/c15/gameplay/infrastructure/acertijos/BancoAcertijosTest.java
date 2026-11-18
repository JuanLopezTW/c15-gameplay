package co.eci.c15.gameplay.infrastructure.acertijos;

import co.eci.c15.gameplay.domain.DefinicionAcertijo;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.TipoComponente;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BancoAcertijosTest {

    private final BancoAcertijos banco = new BancoAcertijos(true);

    private static Stream<DefinicionAcertijo> todos() {
        return Stream.of(ContenidoAcertijos.PREGUNTAS, ContenidoAcertijos.GLOBOS, ContenidoAcertijos.ESTANTES,
                ContenidoAcertijos.CIFRADOS, ContenidoAcertijos.RELOJES, ContenidoAcertijos.CUADROS).flatMap(List::stream);
    }

    @Test
    void elMismoMatchIdDaSiempreLosMismosAcertijos() {
        assertEquals(banco.deLaPartida("partida-1"), new BancoAcertijos(true).deLaPartida("partida-1"));
        assertEquals(banco.deLaPartida("partida-1").get(1), banco.de("partida-1", "acertijo-2").orElseThrow());
    }

    @Test
    void cadaPartidaTieneTresAcertijosDeFamiliasDistintas() {
        for (int i = 0; i < 200; i++) {
            List<DefinicionAcertijo> elegidos = banco.deLaPartida("partida-" + i);
            assertEquals(3, elegidos.size());
            assertEquals(3, elegidos.stream().map(DefinicionAcertijo::tipo).distinct().count(), "partida-" + i);
        }
    }

    @Test
    void conVariasPartidasSalenTodosLosTipos() {
        Set<String> tipos = new HashSet<>();
        IntStream.range(0, 200).forEach(i -> banco.deLaPartida("p" + i).forEach(a -> tipos.add(a.tipo())));
        assertEquals(Set.of("pregunta", "globo", "estante", "cifrado", "reloj", "uv"), tipos);
    }

    @Test
    void laLamparaSoloApareceSiLaPartidaTieneElCuadro() {
        for (int i = 0; i < 100; i++) {
            String partida = "p" + i;
            boolean conCuadro = banco.deLaPartida(partida).stream().anyMatch(a -> a.tipo().equals("uv"));
            assertEquals(conCuadro ? List.of(ContenidoAcertijos.LAMPARA_UV) : List.of(), banco.objetosDe(partida));
        }
    }

    @Test
    void elMapaTraeLaLamparaCuandoHaceFalta() {
        String partida = IntStream.range(0, 100).mapToObj(i -> "p" + i)
                .filter(p -> !banco.objetosDe(p).isEmpty()).findFirst().orElseThrow();
        var mapa = new GeneradorMapa(banco::objetosDe).generar(partida);
        assertTrue(mapa.getComponentes().stream()
                .anyMatch(c -> c.tipo() == TipoComponente.OBJETO && c.id().equals(ContenidoAcertijos.LAMPARA_UV)));
        assertTrue(mapa.todosAccesibles());
    }

    @Test
    void sinSorteoSonSiempreLasTresPreguntasDeSiempre() {
        BancoAcertijos fijo = new BancoAcertijos(false);
        assertEquals(ContenidoAcertijos.PREGUNTAS, fijo.deLaPartida("cualquiera"));
        assertTrue(fijo.de("x", "acertijo-1").orElseThrow().acepta(List.of("16")));
        assertTrue(fijo.de("x", "acertijo-3").orElseThrow().acepta(List.of("ESCAPE")));
        assertTrue(fijo.objetosDe("x").isEmpty());
    }

    @Test
    void componentesQueNoSonAcertijosNoTienenContenido() {
        assertTrue(banco.de("p", "acertijo-4").isEmpty());
        assertTrue(banco.de("p", "acertijo-0").isEmpty());
        assertTrue(banco.de("p", "acertijo-x").isEmpty());
        assertTrue(banco.de("p", "llave-1").isEmpty());
        assertTrue(banco.de(null, "acertijo-1").isEmpty());
        assertTrue(banco.de("p", null).isEmpty());
    }

    @Test
    void cadaAcertijoAceptaSusRespuestasYRechazaOtras() {
        todos().forEach(a -> {
            a.soluciones().forEach(s -> assertTrue(a.acepta(s), a.id()));
            assertFalse(a.acepta(List.of("respuesta equivocada")), a.id());
        });
    }

    @Test
    void lasRespuestasNoDistinguenTildesNiMayusculas() {
        DefinicionAcertijo bogota = ContenidoAcertijos.GLOBOS.get(0);
        assertTrue(bogota.acepta(List.of("BOGOTA")));
        assertTrue(bogota.acepta(List.of("  bogotá ")));
        DefinicionAcertijo cifrado = ContenidoAcertijos.CIFRADOS.get(1);
        assertTrue(cifrado.acepta(List.of("La Luna Salio")));
    }

    @Test
    void losIdsSonUnicosYTodosTienenUnElemento() {
        List<DefinicionAcertijo> lista = todos().toList();
        assertEquals(lista.size(), lista.stream().map(DefinicionAcertijo::id).distinct().count());
        lista.forEach(a -> assertEquals(1, a.elementos().size(), a.id()));
    }

    @Test
    void lasPistasCooperativasEstanRepartidasEnDosGrupos() {
        Stream.concat(ContenidoAcertijos.CIFRADOS.stream(), ContenidoAcertijos.RELOJES.stream()).forEach(a -> {
            assertEquals(2, a.datosPorJugador().size(), a.id());
            Map<String, Object> heroe = a.datosPara(0, false, false);
            Map<String, Object> verdugo = a.datosPara(1, false, false);
            assertNotEquals(heroe, verdugo, a.id());
        });
    }

    @Test
    void elCuadroSoloMuestraElMensajeConLaLampara() {
        DefinicionAcertijo cuadro = ContenidoAcertijos.CUADROS.get(0);
        assertEquals(ContenidoAcertijos.LAMPARA_UV, cuadro.objetoRequerido());
        assertFalse(cuadro.datosPara(0, false, false).containsKey("oculto"));
        assertTrue(cuadro.datosPara(0, false, true).containsKey("oculto"));
    }
}
