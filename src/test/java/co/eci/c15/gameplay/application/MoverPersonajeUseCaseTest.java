package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.MovimientoInvalidoException;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.infrastructure.persistence.JugadorEnMapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MoverPersonajeUseCaseTest {

    private static final String MATCH = "partida-1";

    private JugadorEnMapaRepositoryEnMemoria jugadores;
    private ObtenerMapaUseCase obtenerMapa;
    private List<Object> publicados;
    private MoverPersonajeUseCase mover;
    private MapaIsometrico mapa;

    @BeforeEach
    void setUp() {
        jugadores = new JugadorEnMapaRepositoryEnMemoria();
        obtenerMapa = new ObtenerMapaUseCase(new MapaRepositoryEnMemoria(), new GeneradorMapa());
        publicados = new ArrayList<>();
        mover = new MoverPersonajeUseCase(jugadores, obtenerMapa, publicados::add, CandadoEquipo.ninguno());
        new RegistrarJugadoresUseCase(jugadores, obtenerMapa).ejecutar(MATCH, Map.of("e1", List.of("u1", "u2")));
        mapa = obtenerMapa.obtener(MATCH);
    }

    private Posicion celdaLibreAlcanzable() {
        for (int x = 0; x < mapa.getAncho(); x++) {
            for (int y = 0; y < mapa.getAlto(); y++) {
                Posicion p = new Posicion(x, y);
                if (mapa.esTransitable(p) && !p.equals(mapa.getSpawn())
                        && mapa.rutaEntre(mapa.getSpawn(), p).isPresent()) return p;
            }
        }
        throw new IllegalStateException("mapa sin celdas libres");
    }

    @Test
    void registrarColocaATodosEnElSpawn() {
        assertEquals(mapa.getSpawn(), jugadores.find(MATCH, "u1").orElseThrow().getPosicion());
        assertEquals(mapa.getSpawn(), jugadores.find(MATCH, "u2").orElseThrow().getPosicion());
        assertEquals(2, jugadores.findByMatchId(MATCH).size());
    }

    @Test
    void registrarDosVecesNoReiniciaLaPosicion() {
        Posicion destino = celdaLibreAlcanzable();
        mover.ejecutar(MATCH, "u1", destino);

        new RegistrarJugadoresUseCase(jugadores, obtenerMapa).ejecutar(MATCH, Map.of("e1", List.of("u1")));

        assertEquals(destino, jugadores.find(MATCH, "u1").orElseThrow().getPosicion());
    }

    @Test
    void moverActualizaPosicionDevuelveRutaYPublicaEvento() {
        Posicion destino = celdaLibreAlcanzable();

        MovimientoDto dto = mover.ejecutar(MATCH, "u1", destino);

        assertEquals(mapa.getSpawn(), dto.desde());
        assertEquals(destino, dto.hasta());
        assertEquals(destino, dto.ruta().get(dto.ruta().size() - 1));
        assertEquals(destino, jugadores.find(MATCH, "u1").orElseThrow().getPosicion());
        assertEquals(List.of(new PosicionActualizadaEvent(MATCH, "e1", "u1", destino, dto.ruta())), publicados);
    }

    @Test
    void moverUnJugadorNoAfectaAlOtro() {
        mover.ejecutar(MATCH, "u1", celdaLibreAlcanzable());
        assertEquals(mapa.getSpawn(), jugadores.find(MATCH, "u2").orElseThrow().getPosicion());
    }

    @Test
    void jugadorNoRegistradoLanzaExcepcion() {
        Posicion libre = celdaLibreAlcanzable();
        assertThrows(JugadorNoRegistradoException.class, () -> mover.ejecutar(MATCH, "fantasma", libre));
        assertThrows(JugadorNoRegistradoException.class, () -> mover.ejecutar("otra", "u1", libre));
    }

    @Test
    void clickFueraDelMapaOSobreComponenteNoMueveNiPublica() {
        ComponenteMapa componente = mapa.getComponentes().get(0);

        Posicion fueraDelMapa = new Posicion(99, 99);
        Posicion ocupada = componente.posicion();
        assertThrows(MovimientoInvalidoException.class, () -> mover.ejecutar(MATCH, "u1", fueraDelMapa));
        assertThrows(MovimientoInvalidoException.class, () -> mover.ejecutar(MATCH, "u1", ocupada));

        assertEquals(mapa.getSpawn(), jugadores.find(MATCH, "u1").orElseThrow().getPosicion());
        assertTrue(publicados.isEmpty());
    }

    @Test
    void movimientosConcurrentesDelMismoJugadorTerminanEnUnDestinoConsistente() throws Exception {
        Set<Posicion> destinos = new HashSet<>();
        for (int x = 0; x < mapa.getAncho() && destinos.size() < 8; x++) {
            for (int y = 0; y < mapa.getAlto() && destinos.size() < 8; y++) {
                Posicion p = new Posicion(x, y);
                if (mapa.esTransitable(p) && mapa.rutaEntre(mapa.getSpawn(), p).isPresent()) destinos.add(p);
            }
        }
        ExecutorService pool = Executors.newFixedThreadPool(destinos.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<MovimientoDto>> futuros = new ArrayList<>();
        for (Posicion destino : destinos) {
            futuros.add(pool.submit(() -> {
                salida.await();
                return mover.ejecutar(MATCH, "u1", destino);
            }));
        }
        salida.countDown();
        List<MovimientoDto> resultados = new ArrayList<>();
        for (Future<MovimientoDto> f : futuros) resultados.add(f.get(5, TimeUnit.SECONDS));
        pool.shutdownNow();

        // Cada movimiento parte de donde termino el anterior: la cadena de posiciones es continua.
        Set<Posicion> desdes = new HashSet<>();
        Set<Posicion> hastas = new HashSet<>();
        resultados.forEach(r -> { desdes.add(r.desde()); hastas.add(r.hasta()); });
        assertEquals(resultados.size(), hastas.size());
        JugadorEnMapa jugador = jugadores.find(MATCH, "u1").orElseThrow();
        assertTrue(hastas.contains(jugador.getPosicion()));
        desdes.remove(mapa.getSpawn());
        assertTrue(hastas.containsAll(desdes), "todo origen es el destino de otro movimiento");
        assertEquals(resultados.size(), publicados.size());
    }
}
