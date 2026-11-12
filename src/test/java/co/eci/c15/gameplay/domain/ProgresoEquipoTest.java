package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProgresoEquipoTest {

    @Test
    void empiezaEnCeroYSumaAcertijosEnOrden() {
        ProgresoEquipo p = new ProgresoEquipo("m1", "e1", 3);
        assertEquals(0, p.cantidadResueltos());
        assertFalse(p.estaCompleto());

        assertTrue(p.registrarResuelto("acertijo-2"));
        assertTrue(p.registrarResuelto("acertijo-1"));

        assertEquals(2, p.cantidadResueltos());
        assertEquals(List.of("acertijo-2", "acertijo-1"), p.getResueltos());
    }

    @Test
    void repetirUnAcertijoNoLoCuentaDeNuevo() {
        ProgresoEquipo p = new ProgresoEquipo("m1", "e1", 3);
        assertTrue(p.registrarResuelto("acertijo-1"));
        assertFalse(p.registrarResuelto("acertijo-1"));
        assertEquals(1, p.cantidadResueltos());
    }

    @Test
    void quedaCompletoAlResolverTodos() {
        ProgresoEquipo p = new ProgresoEquipo("m1", "e1", 2);
        p.registrarResuelto("a");
        assertFalse(p.estaCompleto());
        p.registrarResuelto("b");
        assertTrue(p.estaCompleto());
    }

    @Test
    void unMapaSinAcertijosNuncaEstaCompleto() {
        assertFalse(new ProgresoEquipo("m1", "e1", 0).estaCompleto());
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new ProgresoEquipo("m1", "e1", -1));
        assertThrows(NullPointerException.class, () -> new ProgresoEquipo(null, "e1", 1));
        assertThrows(NullPointerException.class, () -> new ProgresoEquipo("m1", "e1", 1).registrarResuelto(null));
    }

    @Test
    void laListaDevueltaEsUnaCopia() {
        ProgresoEquipo p = new ProgresoEquipo("m1", "e1", 3);
        p.registrarResuelto("a");
        assertThrows(UnsupportedOperationException.class, () -> p.getResueltos().add("trampa"));
    }

    @Test
    void elMismoAcertijoDesdeVariosHilosSoloSeCuentaUnaVez() throws Exception {
        ProgresoEquipo p = new ProgresoEquipo("m1", "e1", 3);
        int hilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger nuevos = new AtomicInteger();
        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                salida.await();
                if (p.registrarResuelto("acertijo-1")) nuevos.incrementAndGet();
                return null;
            });
        }
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, nuevos.get());
        assertEquals(1, p.cantidadResueltos());
    }
}
