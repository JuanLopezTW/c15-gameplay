package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.MapaNoGeneradoException;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ObtenerMapaUseCaseTest {

    @Test
    void primeraLlamadaGeneraYLasSiguientesDevuelvenElMismoMapa() {
        ObtenerMapaUseCase useCase = new ObtenerMapaUseCase(new MapaRepositoryEnMemoria(), new GeneradorMapa());

        MapaDto primero = useCase.ejecutar("partida-1");
        MapaDto segundo = useCase.ejecutar("partida-1");

        assertEquals(primero, segundo);
        assertEquals("partida-1", primero.matchId());
        assertFalse(primero.componentes().isEmpty());
    }

    @Test
    void cadaPartidaTieneSuPropioMapa() {
        ObtenerMapaUseCase useCase = new ObtenerMapaUseCase(new MapaRepositoryEnMemoria(), new GeneradorMapa());
        assertNotEquals(useCase.ejecutar("a").componentes(), useCase.ejecutar("b").componentes());
    }

    @Test
    void llamadasConcurrentesRecibenElMismoMapa() throws Exception {
        MapaRepositoryEnMemoria repo = new MapaRepositoryEnMemoria();
        ObtenerMapaUseCase useCase = new ObtenerMapaUseCase(repo, new GeneradorMapa());
        int hilos = 16;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<MapaIsometrico>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            Callable<MapaIsometrico> tarea = () -> {
                salida.await();
                return useCase.obtener("partida-concurrente");
            };
            resultados.add(pool.submit(tarea));
        }
        salida.countDown();
        MapaIsometrico referencia = resultados.get(0).get(5, TimeUnit.SECONDS);
        for (Future<MapaIsometrico> r : resultados) {
            assertSame(referencia, r.get(5, TimeUnit.SECONDS));
        }
        pool.shutdownNow();
    }

    @Test
    void propagaErrorDeCargaSinGuardarNada() {
        AtomicInteger intentos = new AtomicInteger();
        GeneradorMapa fallido = new GeneradorMapa() {
            @Override
            public MapaIsometrico generar(String matchId) {
                intentos.incrementAndGet();
                throw new MapaNoGeneradoException("sin mapa");
            }
        };
        MapaRepositoryEnMemoria repo = new MapaRepositoryEnMemoria();
        ObtenerMapaUseCase useCase = new ObtenerMapaUseCase(repo, fallido);

        assertThrows(MapaNoGeneradoException.class, () -> useCase.ejecutar("partida-1"));
        assertThrows(MapaNoGeneradoException.class, () -> useCase.ejecutar("partida-1"));
        assertEquals(2, intentos.get());
        assertTrue(repo.findByMatchId("partida-1").isEmpty());
    }

    @Test
    void rechazaMatchIdVacio() {
        ObtenerMapaUseCase useCase = new ObtenerMapaUseCase(new MapaRepositoryEnMemoria(), new GeneradorMapa());
        assertThrows(IllegalArgumentException.class, () -> useCase.ejecutar(" "));
        assertThrows(IllegalArgumentException.class, () -> useCase.ejecutar(null));
    }
}
