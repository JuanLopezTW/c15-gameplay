package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GeneradorMapaTest {

    private final GeneradorMapa generador = new GeneradorMapa();

    @Test
    void mismoMatchIdGeneraElMismoMapa() {
        MapaIsometrico a = generador.generar("partida-1");
        MapaIsometrico b = generador.generar("partida-1");
        assertEquals(a.getComponentes(), b.getComponentes());
        assertEquals(a.getSpawn(), b.getSpawn());
    }

    @Test
    void partidasDistintasGeneranMapasDistintos() {
        assertNotEquals(generador.generar("partida-1").getComponentes(),
                generador.generar("partida-2").getComponentes());
    }

    @Test
    void generaLaCantidadEsperadaDeComponentesPorTipo() {
        MapaIsometrico mapa = generador.generar("partida-1");
        assertEquals(GeneradorMapa.ACERTIJOS, contar(mapa, TipoComponente.ACERTIJO));
        assertEquals(GeneradorMapa.LLAVES, contar(mapa, TipoComponente.LLAVE));
        assertEquals(GeneradorMapa.PUERTAS, contar(mapa, TipoComponente.PUERTA));
        assertEquals(GeneradorMapa.OBSTACULOS, contar(mapa, TipoComponente.OBSTACULO));
    }

    @Test
    void componentesEstanDentroSinSolaparseNiOcuparElSpawn() {
        for (int i = 0; i < 200; i++) {
            MapaIsometrico mapa = generador.generar(UUID.randomUUID().toString());
            Set<Posicion> usadas = new HashSet<>();
            Set<String> ids = new HashSet<>();
            for (ComponenteMapa c : mapa.getComponentes()) {
                assertTrue(mapa.dentro(c.posicion()));
                assertNotEquals(mapa.getSpawn(), c.posicion());
                assertTrue(usadas.add(c.posicion()), "celda repetida");
                assertTrue(ids.add(c.id()), "id repetido");
            }
        }
    }

    @Test
    void todosLosMapasGeneradosSonResolubles() {
        for (int i = 0; i < 200; i++) {
            assertTrue(generador.generar("p-" + i).todosAccesibles());
        }
    }

    @Test
    void usaIdsDeAcertijoQueElResultoDelJuegoPuedeReferenciar() {
        MapaIsometrico mapa = generador.generar("partida-1");
        Set<String> ids = new HashSet<>();
        mapa.getComponentes().stream().filter(c -> c.tipo() == TipoComponente.ACERTIJO).forEach(c -> ids.add(c.id()));
        assertEquals(Set.of("acertijo-1", "acertijo-2", "acertijo-3"), ids);
    }

    @Test
    void rechazaMatchIdVacio() {
        assertThrows(IllegalArgumentException.class, () -> generador.generar(" "));
        assertThrows(IllegalArgumentException.class, () -> generador.generar(null));
    }

    private static long contar(MapaIsometrico mapa, TipoComponente tipo) {
        return mapa.getComponentes().stream().filter(c -> c.tipo() == tipo).count();
    }
}
