package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.ObjetoLejosException;
import co.eci.c15.gameplay.domain.ObjetoNoRecolectableException;
import co.eci.c15.gameplay.domain.ObjetoYaRecolectadoException;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.domain.TipoComponente;
import co.eci.c15.gameplay.infrastructure.persistence.InventarioRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.JugadorEnMapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class InventarioUseCasesTest {

    private static final String MATCH = "m1";
    /** Celda vecina de llave-1 (2,2). */
    private static final Posicion JUNTO_A_LLAVE_1 = new Posicion(2, 3);

    private JugadorEnMapaRepositoryEnMemoria jugadores;
    private List<InventarioDto> avisos;
    private RecolectarObjetoUseCase recolectar;
    private ConsultarInventarioUseCase consultar;

    @BeforeEach
    void setUp() {
        MapaRepositoryEnMemoria mapas = new MapaRepositoryEnMemoria();
        mapas.saveIfAbsent(new MapaIsometrico(MATCH, 6, 6, new Posicion(0, 0), List.of(
                new ComponenteMapa("llave-1", TipoComponente.LLAVE, new Posicion(2, 2)),
                new ComponenteMapa("llave-2", TipoComponente.LLAVE, new Posicion(4, 4)),
                new ComponenteMapa("acertijo-1", TipoComponente.ACERTIJO, new Posicion(1, 4)))));
        ObtenerMapaUseCase obtenerMapa = new ObtenerMapaUseCase(mapas, new GeneradorMapa());
        jugadores = new JugadorEnMapaRepositoryEnMemoria();
        avisos = Collections.synchronizedList(new ArrayList<>());
        InventarioRepositoryEnMemoria inventarios = new InventarioRepositoryEnMemoria();
        recolectar = new RecolectarObjetoUseCase(jugadores, obtenerMapa, inventarios, avisos::add, CandadoEquipo.ninguno());
        consultar = new ConsultarInventarioUseCase(jugadores, inventarios);
    }

    private JugadorEnMapa jugador(String equipo, String userId, Posicion posicion) {
        return jugadores.saveIfAbsent(new JugadorEnMapa(MATCH, equipo, userId, posicion));
    }

    @Test
    void recolectarAgregaElObjetoAlInventarioDelEquipo() {
        jugador("e1", "ana", JUNTO_A_LLAVE_1);

        InventarioDto dto = recolectar.ejecutar(MATCH, "ana", "llave-1");

        assertEquals("e1", dto.equipoId());
        assertEquals(List.of(new InventarioDto.ObjetoDto("llave-1", "Llave 1", "ana")), dto.objetos());
    }

    @Test
    void elCompaneroVeLoQueRecolectoOtro() {
        jugador("e1", "ana", JUNTO_A_LLAVE_1);
        jugador("e1", "beto", new Posicion(0, 0));

        recolectar.ejecutar(MATCH, "ana", "llave-1");

        InventarioDto delCompanero = consultar.ejecutar(MATCH, "e1", "beto");
        assertEquals(List.of("llave-1"), delCompanero.objetos().stream().map(InventarioDto.ObjetoDto::id).toList());
        assertEquals("ana", delCompanero.objetos().get(0).recolectadoPor());
    }

    @Test
    void cadaCambioSeAvisaUnaVezConElInventarioCompleto() {
        jugador("e1", "ana", JUNTO_A_LLAVE_1);
        jugador("e1", "beto", new Posicion(4, 3));

        recolectar.ejecutar(MATCH, "ana", "llave-1");
        recolectar.ejecutar(MATCH, "beto", "llave-2");

        assertEquals(2, avisos.size());
        assertEquals(1, avisos.get(0).objetos().size());
        assertEquals(List.of("llave-1", "llave-2"), avisos.get(1).objetos().stream().map(InventarioDto.ObjetoDto::id).toList());
        assertTrue(avisos.stream().allMatch(a -> a.equipoId().equals("e1") && a.matchId().equals(MATCH)));
    }

    @Test
    void elInventarioEsDelEquipoYNoDelOtro() {
        jugador("e1", "ana", JUNTO_A_LLAVE_1);
        jugador("e2", "carla", JUNTO_A_LLAVE_1);

        recolectar.ejecutar(MATCH, "ana", "llave-1");

        assertTrue(consultar.ejecutar(MATCH, "e2", "carla").objetos().isEmpty());
        // el otro equipo puede recolectar su propia llave-1
        assertDoesNotThrow(() -> recolectar.ejecutar(MATCH, "carla", "llave-1"));
        assertEquals(2, avisos.size());
    }

    @Test
    void siElEquipoYaLoTieneNoSeRecolectaDeNuevoNiSeAvisa() {
        jugador("e1", "ana", JUNTO_A_LLAVE_1);
        jugador("e1", "beto", JUNTO_A_LLAVE_1);
        recolectar.ejecutar(MATCH, "ana", "llave-1");
        avisos.clear();

        assertThrows(ObjetoYaRecolectadoException.class, () -> recolectar.ejecutar(MATCH, "beto", "llave-1"));

        assertTrue(avisos.isEmpty());
        assertEquals("ana", consultar.ejecutar(MATCH, "e1", "beto").objetos().get(0).recolectadoPor());
    }

    @Test
    void hayQueEstarJuntoAlObjeto() {
        jugador("e1", "ana", new Posicion(0, 0));
        jugador("e1", "beto", new Posicion(3, 3)); // diagonal: no cuenta como junto

        assertThrows(ObjetoLejosException.class, () -> recolectar.ejecutar(MATCH, "ana", "llave-1"));
        assertThrows(ObjetoLejosException.class, () -> recolectar.ejecutar(MATCH, "beto", "llave-1"));
        assertTrue(avisos.isEmpty());
    }

    @Test
    void soloSeRecolectanLlavesQueExistenEnElMapa() {
        jugador("e1", "ana", new Posicion(1, 3));

        assertThrows(ObjetoNoRecolectableException.class, () -> recolectar.ejecutar(MATCH, "ana", "acertijo-1"));
        assertThrows(ObjetoNoRecolectableException.class, () -> recolectar.ejecutar(MATCH, "ana", "llave-99"));
        assertTrue(avisos.isEmpty());
    }

    @Test
    void jugadorFueraDeLaPartidaNoPuedeRecolectarNiConsultar() {
        assertThrows(JugadorNoRegistradoException.class, () -> recolectar.ejecutar(MATCH, "intruso", "llave-1"));
        assertThrows(JugadorNoRegistradoException.class, () -> consultar.ejecutar(MATCH, "e1", "intruso"));
    }

    @Test
    void nadieVeElInventarioDeOtroEquipo() {
        jugador("e1", "ana", JUNTO_A_LLAVE_1);
        assertThrows(AccesoEquipoDenegadoException.class, () -> consultar.ejecutar(MATCH, "e2", "ana"));
    }

    @Test
    void elInventarioNuevoEstaVacio() {
        jugador("e1", "ana", JUNTO_A_LLAVE_1);
        assertTrue(consultar.ejecutar(MATCH, "e1", "ana").objetos().isEmpty());
    }

    @Test
    void generaElNombreLegibleDelObjeto() {
        assertEquals("Llave 1", RecolectarObjetoUseCase.nombreDe("llave-1"));
        assertEquals("Llave", RecolectarObjetoUseCase.nombreDe("llave"));
    }

    @Test
    void dosCompanerosQueRecolectanLaMismaLlaveALaVezSoloUnoLaConsigueYSeAvisaUnaVez() throws Exception {
        int hilos = 16;
        for (int i = 0; i < hilos; i++) jugador("e1", "j" + i, JUNTO_A_LLAVE_1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            String user = "j" + i;
            Callable<Boolean> tarea = () -> {
                salida.await();
                try {
                    recolectar.ejecutar(MATCH, user, "llave-1");
                    return true;
                } catch (ObjetoYaRecolectadoException e) {
                    return false;
                }
            };
            resultados.add(pool.submit(tarea));
        }
        salida.countDown();
        int ganadores = 0;
        for (Future<Boolean> f : resultados) if (f.get(5, TimeUnit.SECONDS)) ganadores++;
        pool.shutdownNow();

        assertEquals(1, ganadores);
        assertEquals(1, avisos.size());
        assertEquals(1, consultar.ejecutar(MATCH, "e1", "j0").objetos().size());
    }
}
