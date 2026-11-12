package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.application.PartidaOcupadaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CandadoEquipoRedisTest extends RedisDePrueba {

    private CandadoEquipoRedis instanciaA;
    private CandadoEquipoRedis instanciaB;

    @BeforeEach
    void setUp() {
        instanciaA = new CandadoEquipoRedis(redis);
        instanciaB = new CandadoEquipoRedis(redis);
    }

    /** Leer, sumar y guardar en Redis desde dos "instancias" a la vez: sin candado se perderian sumas. */
    @Test
    void dosInstanciasNoPisanLosCambiosDelOtro() throws Exception {
        int porInstancia = 40;
        ExecutorService hilos = Executors.newFixedThreadPool(8);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> tareas = new ArrayList<>();
        for (int i = 0; i < porInstancia * 2; i++) {
            CandadoEquipoRedis candado = i % 2 == 0 ? instanciaA : instanciaB;
            tareas.add(hilos.submit(() -> {
                largada.await();
                candado.ejecutar("m1", "e1", () -> {
                    String actual = redis.opsForValue().get("contador");
                    int valor = actual == null ? 0 : Integer.parseInt(actual);
                    redis.opsForValue().set("contador", String.valueOf(valor + 1));
                });
                return null;
            }));
        }
        largada.countDown();
        for (Future<?> t : tareas) t.get(30, TimeUnit.SECONDS);
        hilos.shutdown();

        assertEquals(String.valueOf(porInstancia * 2), redis.opsForValue().get("contador"));
        assertFalse(redis.hasKey(CandadoEquipoRedis.clave("m1", "e1")), "el candado queda libre");
    }

    @Test
    void equiposDistintosNoSeEsperanEntreSi() {
        String resultado = instanciaA.ejecutar("m1", "e1", () -> instanciaB.ejecutar("m1", "e2", () -> "sin esperar"));

        assertEquals("sin esperar", resultado);
    }

    @Test
    void esReentranteEnElMismoHilo() {
        String resultado = instanciaA.ejecutar("m1", "e1", () -> instanciaA.ejecutar("m1", "e1", () -> "adentro"));

        assertEquals("adentro", resultado);
    }

    @Test
    void siOtroLoTieneDemasiadoTiempoSeRindeYAvisa() {
        redis.opsForValue().set(CandadoEquipoRedis.clave("m1", "e1"), "de-otra-instancia",
                CandadoEquipoRedis.VIDA);

        long inicio = System.nanoTime();
        assertThrows(PartidaOcupadaException.class, () -> instanciaA.ejecutar("m1", "e1", () -> "nunca"));
        long esperado = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        assertTrue(esperado >= CandadoEquipoRedis.ESPERA_MAXIMA.toMillis() - 50, "espero " + esperado + " ms");
        assertEquals("de-otra-instancia", redis.opsForValue().get(CandadoEquipoRedis.clave("m1", "e1")),
                "no le borra el candado al otro");
    }

    @Test
    void unErrorAdentroLiberaElCandado() {
        assertThrows(IllegalStateException.class, () -> instanciaA.ejecutar("m1", "e1", () -> {
            throw new IllegalStateException("falla de la regla");
        }));

        assertEquals("libre", instanciaB.ejecutar("m1", "e1", () -> "libre"));
    }

    @Test
    void elCandadoCaducaSiLaInstanciaMuere() {
        instanciaA.ejecutar("m1", "e1", () -> {
            long vida = redis.getExpire(CandadoEquipoRedis.clave("m1", "e1"), TimeUnit.MILLISECONDS);
            assertTrue(vida > 0 && vida <= CandadoEquipoRedis.VIDA.toMillis(), "caduca en " + vida + " ms");
            return null;
        });
    }
}
