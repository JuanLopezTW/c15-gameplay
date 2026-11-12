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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class AcertijoResolverTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final Predicate<List<String>> ES_16 = r -> r.equals(List.of("16"));

    private AcertijoEnPartida acertijo;

    @BeforeEach
    void setUp() {
        acertijo = new AcertijoEnPartida("m1", "e1", "acertijo-1");
    }

    private void abrirPara(String userId) {
        acertijo.bloquear(userId, T0, Duration.ofMinutes(5), u -> true);
    }

    @Test
    void respuestaCorrectaResuelveYLiberaElAcertijo() {
        abrirPara("ana");

        AcertijoEnPartida.Resultado r = acertijo.intentarResolver("ana", List.of("16"), ES_16);

        assertEquals(AcertijoEnPartida.Resultado.CORRECTA, r);
        assertTrue(acertijo.isResuelto());
        assertTrue(acertijo.getPoseedor().isEmpty());
    }

    @Test
    void sinRespuestaExplicitaSeEvaluanLosClicks() {
        abrirPara("ana");
        acertijo.registrarClick("ana", "16");

        assertEquals(AcertijoEnPartida.Resultado.CORRECTA, acertijo.intentarResolver("ana", null, ES_16));
    }

    @Test
    void respuestaIncorrectaDescartaLaEntradaYSigueAbierto() {
        abrirPara("ana");
        acertijo.registrarClick("ana", "17");

        AcertijoEnPartida.Resultado r = acertijo.intentarResolver("ana", null, ES_16);

        assertEquals(AcertijoEnPartida.Resultado.INCORRECTA, r);
        assertFalse(acertijo.isResuelto());
        assertTrue(acertijo.getEntrada().isEmpty());
        assertEquals("ana", acertijo.getPoseedor().orElseThrow());
        assertEquals(AcertijoEnPartida.Resultado.CORRECTA, acertijo.intentarResolver("ana", List.of("16"), ES_16));
    }

    @Test
    void sinClicksNiRespuestaEsIncorrecto() {
        abrirPara("ana");
        assertEquals(AcertijoEnPartida.Resultado.INCORRECTA, acertijo.intentarResolver("ana", null, r -> !r.isEmpty()));
    }

    @Test
    void soloQuienLoTieneAbiertoPuedeEnviarLaSolucion() {
        assertThrows(AcertijoNoAbiertoException.class, () -> acertijo.intentarResolver("ana", List.of("16"), ES_16));
        abrirPara("ana");
        assertThrows(AcertijoBloqueadoException.class, () -> acertijo.intentarResolver("beto", List.of("16"), ES_16));
        assertFalse(acertijo.isResuelto());
    }

    @Test
    void yaResueltoNoSePuedeVolverAResolver() {
        abrirPara("ana");
        acertijo.intentarResolver("ana", List.of("16"), ES_16);
        assertThrows(AcertijoYaResueltoException.class, () -> acertijo.intentarResolver("ana", List.of("16"), ES_16));
    }

    @Test
    void conVariosIntentosCorrectosALaVezSoloUnoResuelve() throws Exception {
        abrirPara("ana");
        int hilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger correctas = new AtomicInteger();
        AtomicInteger yaResueltas = new AtomicInteger();
        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                salida.await();
                try {
                    if (acertijo.intentarResolver("ana", List.of("16"), ES_16) == AcertijoEnPartida.Resultado.CORRECTA) {
                        correctas.incrementAndGet();
                    }
                } catch (AcertijoYaResueltoException e) {
                    yaResueltas.incrementAndGet();
                }
                return null;
            });
        }
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, correctas.get());
        assertEquals(hilos - 1, yaResueltas.get());
    }
}
