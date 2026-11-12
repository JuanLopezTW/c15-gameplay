package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InventarioEquipoTest {

    private static ObjetoInventario objeto(String id, String user) {
        return new ObjetoInventario(id, "Llave", user);
    }

    @Test
    void empiezaVacio() {
        assertTrue(new InventarioEquipo("m1", "e1").getObjetos().isEmpty());
    }

    @Test
    void conservaLosObjetosEnElOrdenDeRecoleccion() {
        InventarioEquipo inv = new InventarioEquipo("m1", "e1");

        assertTrue(inv.agregar(objeto("llave-2", "ana")));
        assertTrue(inv.agregar(objeto("llave-1", "beto")));

        assertEquals(List.of("llave-2", "llave-1"), inv.getObjetos().stream().map(ObjetoInventario::id).toList());
    }

    @Test
    void unObjetoSoloSeAgregaUnaVezYConservaAQuienLoRecolecto() {
        InventarioEquipo inv = new InventarioEquipo("m1", "e1");
        inv.agregar(objeto("llave-1", "ana"));

        assertFalse(inv.agregar(objeto("llave-1", "beto")));

        assertEquals(1, inv.getObjetos().size());
        assertEquals("ana", inv.getObjetos().get(0).recolectadoPor());
    }

    @Test
    void laListaDevueltaEsUnaCopia() {
        InventarioEquipo inv = new InventarioEquipo("m1", "e1");
        inv.agregar(objeto("llave-1", "ana"));
        assertThrows(UnsupportedOperationException.class, () -> inv.getObjetos().clear());
    }

    @Test
    void rechazaObjetosIncompletos() {
        assertThrows(NullPointerException.class, () -> new ObjetoInventario(null, "x", "u"));
        assertThrows(NullPointerException.class, () -> new InventarioEquipo(null, "e1"));
    }

    @Test
    void variosCompanerosRecolectandoElMismoObjetoSoloUnoLoConsigue() throws Exception {
        InventarioEquipo inv = new InventarioEquipo("m1", "e1");
        int hilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger ganadores = new AtomicInteger();
        for (int i = 0; i < hilos; i++) {
            String user = "jugador-" + i;
            pool.submit(() -> {
                salida.await();
                if (inv.agregar(objeto("llave-1", user))) ganadores.incrementAndGet();
                return null;
            });
        }
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, ganadores.get());
        assertEquals(1, inv.getObjetos().size());
    }
}
