package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JugadorEnMapaAcertijoTest {

    private static final ComponenteMapa ACERTIJO_1 = new ComponenteMapa("acertijo-1", TipoComponente.ACERTIJO, new Posicion(2, 2));
    private static final ComponenteMapa ACERTIJO_2 = new ComponenteMapa("acertijo-2", TipoComponente.ACERTIJO, new Posicion(2, 4));

    private MapaIsometrico mapa;

    @BeforeEach
    void setUp() {
        mapa = new MapaIsometrico("match-1", 6, 6, new Posicion(0, 0), List.of(ACERTIJO_1, ACERTIJO_2));
    }

    @Test
    void abreSiEstaEnUnaCeldaVecina() {
        JugadorEnMapa jugador = new JugadorEnMapa("match-1", "equipo-1", "ana", new Posicion(2, 3));
        jugador.abrirAcertijo(ACERTIJO_1, () -> { });
        assertEquals(Optional.of("acertijo-1"), jugador.getAcertijoAbierto());
    }

    @Test
    void noAbreSiEstaLejos() {
        JugadorEnMapa jugador = new JugadorEnMapa("match-1", "equipo-1", "ana", new Posicion(0, 0));
        assertThrows(AcertijoLejosException.class, () -> jugador.abrirAcertijo(ACERTIJO_1, () -> { }));
        assertTrue(jugador.getAcertijoAbierto().isEmpty());
    }

    @Test
    void enDiagonalCuentaComoLejos() {
        JugadorEnMapa jugador = new JugadorEnMapa("match-1", "equipo-1", "ana", new Posicion(3, 3));
        assertThrows(AcertijoLejosException.class, () -> jugador.abrirAcertijo(ACERTIJO_1, () -> { }));
    }

    @Test
    void siElBloqueoFallaNoQuedaAbierto() {
        JugadorEnMapa jugador = new JugadorEnMapa("match-1", "equipo-1", "ana", new Posicion(2, 3));
        assertThrows(AcertijoBloqueadoException.class, () -> jugador.abrirAcertijo(ACERTIJO_1, () -> {
            throw new AcertijoBloqueadoException("acertijo-1");
        }));
        assertTrue(jugador.getAcertijoAbierto().isEmpty());
    }

    @Test
    void conUnAcertijoAbiertoNoPuedeMoverse() {
        JugadorEnMapa jugador = new JugadorEnMapa("match-1", "equipo-1", "ana", new Posicion(2, 3));
        jugador.abrirAcertijo(ACERTIJO_1, () -> { });
        assertThrows(MovimientoInvalidoException.class, () -> jugador.mover(mapa, new Posicion(0, 3)));
        assertEquals(new Posicion(2, 3), jugador.getPosicion());
    }

    @Test
    void alCerrarloPuedeVolverAMoverse() {
        JugadorEnMapa jugador = new JugadorEnMapa("match-1", "equipo-1", "ana", new Posicion(2, 3));
        jugador.abrirAcertijo(ACERTIJO_1, () -> { });
        jugador.cerrarAcertijo("acertijo-1");
        assertDoesNotThrow(() -> jugador.mover(mapa, new Posicion(0, 3)));
    }

    @Test
    void noPuedeAbrirDosAcertijosALaVez() {
        // (2,3) es vecina de acertijo-1 (2,2) y de acertijo-2 (2,4)
        JugadorEnMapa jugador = new JugadorEnMapa("match-1", "equipo-1", "ana", new Posicion(2, 3));
        jugador.abrirAcertijo(ACERTIJO_1, () -> { });
        assertThrows(OtroAcertijoAbiertoException.class, () -> jugador.abrirAcertijo(ACERTIJO_2, () -> { }));
    }
}
