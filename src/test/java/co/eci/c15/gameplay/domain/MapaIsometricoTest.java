package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapaIsometricoTest {

    private static MapaIsometrico mapa(ComponenteMapa... componentes) {
        return new MapaIsometrico("m1", 4, 4, new Posicion(0, 0), List.of(componentes));
    }

    @Test
    void celdaLibreDentroDelMapaEsTransitable() {
        assertTrue(mapa().esTransitable(new Posicion(2, 2)));
    }

    @Test
    void celdaConComponenteOFueraDelMapaNoEsTransitable() {
        MapaIsometrico m = mapa(new ComponenteMapa("o1", TipoComponente.OBSTACULO, new Posicion(1, 1)));
        assertFalse(m.esTransitable(new Posicion(1, 1)));
        assertFalse(m.esTransitable(new Posicion(4, 0)));
        assertFalse(m.esTransitable(new Posicion(-1, 0)));
        assertFalse(m.esTransitable(null));
    }

    @Test
    void rutaEntreCeldasEvitaObstaculos() {
        MapaIsometrico m = mapa(new ComponenteMapa("o1", TipoComponente.OBSTACULO, new Posicion(1, 0)));
        List<Posicion> ruta = m.rutaEntre(new Posicion(0, 0), new Posicion(2, 0)).orElseThrow();
        assertEquals(new Posicion(2, 0), ruta.get(ruta.size() - 1));
        assertFalse(ruta.contains(new Posicion(1, 0)));
        assertEquals(4, ruta.size());
    }

    @Test
    void rutaHaciaLaMismaCeldaEsVacia() {
        assertEquals(List.of(), mapa().rutaEntre(new Posicion(1, 1), new Posicion(1, 1)).orElseThrow());
    }

    @Test
    void rutaHaciaCeldaBloqueadaOFueraDelMapaNoExiste() {
        MapaIsometrico m = mapa(new ComponenteMapa("o1", TipoComponente.OBSTACULO, new Posicion(1, 1)));
        assertTrue(m.rutaEntre(new Posicion(0, 0), new Posicion(1, 1)).isEmpty());
        assertTrue(m.rutaEntre(new Posicion(0, 0), new Posicion(9, 9)).isEmpty());
    }

    @Test
    void rutaNoExisteSiLaCeldaEstaAislada() {
        MapaIsometrico m = mapa(
                new ComponenteMapa("o1", TipoComponente.OBSTACULO, new Posicion(2, 3)),
                new ComponenteMapa("o2", TipoComponente.OBSTACULO, new Posicion(3, 2)));
        assertTrue(m.rutaEntre(new Posicion(0, 0), new Posicion(3, 3)).isEmpty());
    }

    @Test
    void detectaComponentesInaccesibles() {
        MapaIsometrico encerrado = mapa(
                new ComponenteMapa("l1", TipoComponente.LLAVE, new Posicion(3, 3)),
                new ComponenteMapa("o1", TipoComponente.OBSTACULO, new Posicion(2, 3)),
                new ComponenteMapa("o2", TipoComponente.OBSTACULO, new Posicion(3, 2)));
        assertFalse(encerrado.todosAccesibles());
        assertTrue(mapa(new ComponenteMapa("l1", TipoComponente.LLAVE, new Posicion(3, 3))).todosAccesibles());
    }

    @Test
    void rechazaComponentesInvalidos() {
        Posicion spawn = new Posicion(0, 0);
        assertThrows(IllegalArgumentException.class, () -> new MapaIsometrico("m1", 4, 4, spawn,
                List.of(new ComponenteMapa("a", TipoComponente.LLAVE, new Posicion(5, 5)))));
        assertThrows(IllegalArgumentException.class, () -> new MapaIsometrico("m1", 4, 4, spawn,
                List.of(new ComponenteMapa("a", TipoComponente.LLAVE, spawn))));
        assertThrows(IllegalArgumentException.class, () -> new MapaIsometrico("m1", 4, 4, spawn,
                List.of(new ComponenteMapa("a", TipoComponente.LLAVE, new Posicion(1, 1)),
                        new ComponenteMapa("b", TipoComponente.LLAVE, new Posicion(1, 1)))));
        assertThrows(IllegalArgumentException.class, () -> new MapaIsometrico("m1", 4, 4, spawn,
                List.of(new ComponenteMapa("a", TipoComponente.LLAVE, new Posicion(1, 1)),
                        new ComponenteMapa("a", TipoComponente.LLAVE, new Posicion(2, 2)))));
        assertThrows(IllegalArgumentException.class, () -> new MapaIsometrico(" ", 4, 4, spawn, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new MapaIsometrico("m1", 0, 4, spawn, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new MapaIsometrico("m1", 4, 4, new Posicion(9, 9), List.of()));
    }
}
