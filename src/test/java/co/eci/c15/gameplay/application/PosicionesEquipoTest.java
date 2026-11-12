package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.infrastructure.events.PosicionActualizadaListener;
import co.eci.c15.gameplay.infrastructure.persistence.JugadorEnMapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PosicionesEquipoTest {

    private static final String MATCH = "p1";

    private record Envio(String matchId, String equipoId, PosicionJugadorDto dto) {}

    private JugadorEnMapaRepositoryEnMemoria jugadores;
    private List<Envio> enviados;
    private PosicionNotifier notifier;
    private MoverPersonajeUseCase mover;
    private ConsultarPosicionesEquipoUseCase consultar;
    private DesconectarJugadorUseCase desconectar;
    private Posicion destino;

    @BeforeEach
    void setUp() {
        jugadores = new JugadorEnMapaRepositoryEnMemoria();
        enviados = new ArrayList<>();
        notifier = (m, e, dto) -> enviados.add(new Envio(m, e, dto));
        ObtenerMapaUseCase obtenerMapa = new ObtenerMapaUseCase(new MapaRepositoryEnMemoria(), new GeneradorMapa());
        new RegistrarJugadoresUseCase(jugadores, obtenerMapa)
                .ejecutar(MATCH, Map.of("e1", List.of("a1", "a2"), "e2", List.of("b1")));

        PosicionActualizadaListener listener = new PosicionActualizadaListener(notifier);
        mover = new MoverPersonajeUseCase(jugadores, obtenerMapa, e -> listener.onPosicionActualizada((PosicionActualizadaEvent) e), CandadoEquipo.ninguno());
        consultar = new ConsultarPosicionesEquipoUseCase(jugadores);
        desconectar = new DesconectarJugadorUseCase(jugadores, notifier);

        MapaIsometrico mapa = obtenerMapa.obtener(MATCH);
        destino = new Posicion(0, 1);
        if (!mapa.esTransitable(destino)) destino = new Posicion(1, 0);
    }

    @Test
    void moverseAvisaSoloAlTopicDeSuEquipo() {
        MovimientoDto movimiento = mover.ejecutar(MATCH, "a1", destino);

        assertEquals(1, enviados.size());
        Envio envio = enviados.get(0);
        assertEquals(MATCH, envio.matchId());
        assertEquals("e1", envio.equipoId());
        assertEquals(new PosicionJugadorDto("a1", destino.x(), destino.y(), true, movimiento.ruta()), envio.dto());
        assertEquals(destino, envio.dto().ruta().get(envio.dto().ruta().size() - 1));
    }

    @Test
    void movimientoInvalidoNoAvisaANadie() {
        Posicion fueraDelMapa = new Posicion(99, 99);
        assertThrows(RuntimeException.class, () -> mover.ejecutar(MATCH, "a1", fueraDelMapa));
        assertTrue(enviados.isEmpty());
    }

    @Test
    void consultaDevuelveSoloAlosDelMismoEquipo() {
        mover.ejecutar(MATCH, "a2", destino);

        List<PosicionJugadorDto> posiciones = consultar.ejecutar(MATCH, "e1", "a1");

        assertEquals(List.of("a1", "a2"), posiciones.stream().map(PosicionJugadorDto::userId).toList());
        PosicionJugadorDto a2 = posiciones.get(1);
        assertEquals(new Posicion(destino.x(), destino.y()), new Posicion(a2.x(), a2.y()));
        assertTrue(posiciones.stream().allMatch(PosicionJugadorDto::conectado));
    }

    @Test
    void unJugadorNoPuedeVerOtroEquipo() {
        assertThrows(AccesoEquipoDenegadoException.class, () -> consultar.ejecutar(MATCH, "e2", "a1"));
        assertThrows(AccesoEquipoDenegadoException.class, () -> consultar.ejecutar(MATCH, "e1", "b1"));
    }

    @Test
    void consultaDeJugadorDesconocidoFalla() {
        assertThrows(JugadorNoRegistradoException.class, () -> consultar.ejecutar(MATCH, "e1", "fantasma"));
    }

    @Test
    void desconectarSacaAlJugadorDelMapaYAvisaASuEquipo() {
        mover.ejecutar(MATCH, "a1", destino);
        enviados.clear();

        desconectar.ejecutar(MATCH, "a1");

        assertEquals(1, enviados.size());
        assertEquals("e1", enviados.get(0).equipoId());
        assertEquals(new PosicionJugadorDto("a1", destino.x(), destino.y(), false), enviados.get(0).dto());
        assertEquals(List.of("a2"), consultar.ejecutar(MATCH, "e1", "a2").stream().map(PosicionJugadorDto::userId).toList());
        assertThrows(JugadorNoRegistradoException.class, () -> mover.ejecutar(MATCH, "a1", destino));
    }

    @Test
    void desconectarEsIdempotente() {
        desconectar.ejecutar(MATCH, "a1");
        enviados.clear();

        desconectar.ejecutar(MATCH, "a1");
        desconectar.ejecutar(MATCH, "nadie");
        desconectar.ejecutar("otra", "a1");

        assertTrue(enviados.isEmpty());
    }
}
