package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AcertijoEnPartidaTest {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    private AcertijoEnPartida acertijo;

    @BeforeEach
    void setUp() {
        acertijo = new AcertijoEnPartida("match-1", "equipo-1", "acertijo-1");
    }

    @Test
    void empiezaPendienteYLibre() {
        assertEquals(AcertijoEnPartida.Estado.PENDIENTE, acertijo.getEstado());
        assertTrue(acertijo.getPoseedor().isEmpty());
    }

    @Test
    void bloquearLibreLoAsigna() {
        assertEquals(Optional.empty(), acertijo.bloquear("ana", T0, TTL, u -> true));
        assertEquals(Optional.of("ana"), acertijo.getPoseedor());
    }

    @Test
    void elMismoJugadorPuedeVolverAAbrirlo() {
        acertijo.bloquear("ana", T0, TTL, u -> true);
        assertDoesNotThrow(() -> acertijo.bloquear("ana", T0.plusSeconds(10), TTL, u -> true));
    }

    @Test
    void otroJugadorNoPuedeTomarloMientrasEsteVigente() {
        acertijo.bloquear("ana", T0, TTL, u -> true);
        assertThrows(AcertijoBloqueadoException.class,
                () -> acertijo.bloquear("beto", T0.plusSeconds(60), TTL, u -> true));
        assertEquals(Optional.of("ana"), acertijo.getPoseedor());
    }

    @Test
    void siElBloqueoVencioOtroLoToma() {
        acertijo.bloquear("ana", T0, TTL, u -> true);
        assertEquals(Optional.of("ana"), acertijo.bloquear("beto", T0.plus(TTL), TTL, u -> true));
        assertEquals(Optional.of("beto"), acertijo.getPoseedor());
    }

    @Test
    void siElPoseedorSalioDeLaPartidaOtroLoToma() {
        acertijo.bloquear("ana", T0, TTL, u -> true);
        assertEquals(Optional.of("ana"),
                acertijo.bloquear("beto", T0.plusSeconds(1), TTL, u -> !u.equals("ana")));
    }

    @Test
    void soloElPoseedorLoLibera() {
        acertijo.bloquear("ana", T0, TTL, u -> true);
        acertijo.liberar("beto");
        assertEquals(Optional.of("ana"), acertijo.getPoseedor());
        acertijo.liberar("ana");
        assertTrue(acertijo.getPoseedor().isEmpty());
    }

    @Test
    void marcarResueltoLiberaElBloqueo() {
        acertijo.bloquear("ana", T0, TTL, u -> true);
        acertijo.marcarResuelto();
        assertTrue(acertijo.isResuelto());
        assertTrue(acertijo.getPoseedor().isEmpty());
    }

    @Test
    void conVariosIntentosALaVezSoloUnoLoObtiene() throws Exception {
        int hilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        for (int i = 0; i < hilos; i++) {
            String userId = "jugador-" + i;
            pool.submit(() -> {
                salida.await();
                try {
                    acertijo.bloquear(userId, T0, TTL, u -> true);
                    exitos.incrementAndGet();
                } catch (AcertijoBloqueadoException ignored) {
                    // otro jugador lo obtuvo primero
                }
                return null;
            });
        }
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, exitos.get());
    }
}
