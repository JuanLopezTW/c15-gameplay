package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JugadorEnMapaTest {

    private final MapaIsometrico mapa = new MapaIsometrico("m1", 5, 5, new Posicion(0, 0), List.of(
            new ComponenteMapa("o1", TipoComponente.OBSTACULO, new Posicion(1, 0)),
            new ComponenteMapa("o2", TipoComponente.OBSTACULO, new Posicion(4, 3)),
            new ComponenteMapa("o3", TipoComponente.OBSTACULO, new Posicion(3, 4))));

    private JugadorEnMapa jugador() {
        return new JugadorEnMapa("m1", "e1", "u1", new Posicion(0, 0));
    }

    @Test
    void muevePorUnaRutaValidaYActualizaLaPosicion() {
        JugadorEnMapa jugador = jugador();

        JugadorEnMapa.Movimiento m = jugador.mover(mapa, new Posicion(2, 0));

        assertEquals(new Posicion(0, 0), m.desde());
        assertEquals(new Posicion(2, 0), m.hasta());
        assertEquals(new Posicion(2, 0), jugador.getPosicion());
        assertEquals(new Posicion(2, 0), m.ruta().get(m.ruta().size() - 1));
        assertFalse(m.ruta().contains(new Posicion(1, 0)));
    }

    @Test
    void moverseAlaMismaCeldaDejaRutaVacia() {
        assertTrue(jugador().mover(mapa, new Posicion(0, 0)).ruta().isEmpty());
    }

    @Test
    void rechazaDestinoFueraDelMapa() {
        JugadorEnMapa jugador = jugador();
        assertThrows(MovimientoInvalidoException.class, () -> jugador.mover(mapa, new Posicion(5, 5)));
        assertThrows(MovimientoInvalidoException.class, () -> jugador.mover(mapa, new Posicion(-1, 0)));
        assertEquals(new Posicion(0, 0), jugador.getPosicion());
    }

    @Test
    void rechazaDestinoSobreUnComponente() {
        JugadorEnMapa jugador = jugador();
        assertThrows(MovimientoInvalidoException.class, () -> jugador.mover(mapa, new Posicion(1, 0)));
        assertEquals(new Posicion(0, 0), jugador.getPosicion());
    }

    @Test
    void rechazaDestinoSinCamino() {
        MapaIsometrico cerrado = new MapaIsometrico("m1", 3, 3, new Posicion(0, 0), List.of(
                new ComponenteMapa("o1", TipoComponente.OBSTACULO, new Posicion(1, 2)),
                new ComponenteMapa("o2", TipoComponente.OBSTACULO, new Posicion(2, 1))));
        JugadorEnMapa jugador = jugador();
        assertThrows(MovimientoInvalidoException.class, () -> jugador.mover(cerrado, new Posicion(2, 2)));
    }
}
