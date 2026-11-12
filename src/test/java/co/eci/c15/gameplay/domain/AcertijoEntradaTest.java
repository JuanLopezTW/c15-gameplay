package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AcertijoEntradaTest {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    private AcertijoEnPartida acertijo;

    @BeforeEach
    void setUp() {
        acertijo = new AcertijoEnPartida("m1", "e1", "acertijo-1");
    }

    private void abrirPara(String userId) {
        acertijo.bloquear(userId, T0, TTL, u -> true);
    }

    @Test
    void losClicksSeAcumulanEnOrden() {
        abrirPara("ana");

        acertijo.registrarClick("ana", "rojo");
        List<String> entrada = acertijo.registrarClick("ana", "azul");

        assertEquals(List.of("rojo", "azul"), entrada);
        assertEquals(entrada, acertijo.getEntrada());
    }

    @Test
    void sinAbrirElAcertijoNoSePuedeHacerClick() {
        assertThrows(AcertijoNoAbiertoException.class, () -> acertijo.registrarClick("ana", "rojo"));
        assertTrue(acertijo.getEntrada().isEmpty());
    }

    @Test
    void unCompaneroNoPuedeHacerClickMientrasOtroLoTieneAbierto() {
        abrirPara("ana");
        AcertijoBloqueadoException enUso = assertThrows(AcertijoBloqueadoException.class,
                () -> acertijo.registrarClick("beto", "rojo"));
        assertTrue(enUso.getMessage().contains("otro jugador"));
        assertTrue(acertijo.getEntrada().isEmpty());
    }

    @Test
    void acertijoResueltoNoAceptaMasClicks() {
        abrirPara("ana");
        acertijo.marcarResuelto();
        assertThrows(AcertijoYaResueltoException.class, () -> acertijo.registrarClick("ana", "rojo"));
        assertThrows(AcertijoYaResueltoException.class, () -> acertijo.reiniciarEntrada("ana"));
    }

    @Test
    void reiniciarBorraLaEntradaSoloParaQuienLoTieneAbierto() {
        abrirPara("ana");
        acertijo.registrarClick("ana", "rojo");

        assertThrows(AcertijoBloqueadoException.class, () -> acertijo.reiniciarEntrada("beto"));
        assertEquals(List.of("rojo"), acertijo.getEntrada());

        acertijo.reiniciarEntrada("ana");
        assertTrue(acertijo.getEntrada().isEmpty());
    }

    @Test
    void cerrarElAcertijoDescartaLoIngresado() {
        abrirPara("ana");
        acertijo.registrarClick("ana", "rojo");

        acertijo.liberar("ana");

        assertTrue(acertijo.getEntrada().isEmpty());
    }

    @Test
    void liberarPorOtroJugadorNoBorraLaEntrada() {
        abrirPara("ana");
        acertijo.registrarClick("ana", "rojo");

        acertijo.liberar("beto");

        assertEquals(List.of("rojo"), acertijo.getEntrada());
    }

    @Test
    void siOtroToma_elAcertijoEmpiezaLimpio() {
        abrirPara("ana");
        acertijo.registrarClick("ana", "rojo");

        acertijo.bloquear("beto", T0.plus(TTL), TTL, u -> true);

        assertTrue(acertijo.getEntrada().isEmpty());
        assertThrows(AcertijoBloqueadoException.class, () -> acertijo.registrarClick("ana", "azul"));
    }

    @Test
    void volverAAbrirElMismoJugadorConservaLaEntrada() {
        abrirPara("ana");
        acertijo.registrarClick("ana", "rojo");

        acertijo.bloquear("ana", T0.plusSeconds(10), TTL, u -> true);

        assertEquals(List.of("rojo"), acertijo.getEntrada());
    }

    @Test
    void laEntradaDevueltaEsUnaCopia() {
        abrirPara("ana");
        List<String> copia = acertijo.registrarClick("ana", "rojo");
        assertThrows(UnsupportedOperationException.class, () -> copia.add("trampa"));
        assertThrows(UnsupportedOperationException.class, () -> acertijo.getEntrada().add("trampa"));
    }

    @Test
    void clicksConcurrentesDelMismoJugadorNoSePierden() throws Exception {
        abrirPara("ana");
        int hilos = 20;
        int clicksPorHilo = 50;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                salida.await();
                for (int j = 0; j < clicksPorHilo; j++) acertijo.registrarClick("ana", "x");
                return null;
            });
        }
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(hilos * clicksPorHilo, acertijo.getEntrada().size());
    }

    @Test
    void siSeLiberaEnPleno_clickNingunClickLlegaDespuesDelLiberar() throws Exception {
        abrirPara("ana");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        pool.submit(() -> {
            salida.await();
            for (int i = 0; i < 1000; i++) {
                try {
                    acertijo.registrarClick("ana", "x");
                } catch (AcertijoNoAbiertoException e) {
                    return null; // ya no lo tiene abierto
                }
            }
            return null;
        });
        pool.submit(() -> {
            salida.await();
            acertijo.liberar("ana");
            return null;
        });
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        // Tras liberar, la entrada siempre queda vacia: nada se cuela despues del liberar.
        assertTrue(acertijo.getEntrada().isEmpty());
        assertTrue(acertijo.getPoseedor().isEmpty());
    }
}
