package co.eci.c15.plataforma.caida;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CaidaSimuladaControllerTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(CaidaSimuladaController.class)
            .withPropertyValues("spring.application.name=c15-prueba");

    @Test
    void respondeQuienEsYDespuesSeApagaEnSeco() {
        AtomicInteger codigo = new AtomicInteger(-1);
        CaidaSimuladaController caida = new CaidaSimuladaController("c15-prueba", "replica-1", codigo::set, Duration.ofMillis(300));

        ResponseEntity<Map<String, String>> r = caida.caer();

        assertEquals(202, r.getStatusCode().value());
        assertEquals(Map.of("servicio", "c15-prueba", "replica", "replica-1"), r.getBody());
        assertEquals(-1, codigo.get(), "primero sale la respuesta");
        await().atMost(3, TimeUnit.SECONDS).until(() -> codigo.get() == CaidaSimuladaController.CODIGO_SALIDA);
    }

    @Test
    void soloExisteSiSePrendeParaLaPrueba() {
        contexto.run(c -> assertThat(c).doesNotHaveBean(CaidaSimuladaController.class));
        contexto.withPropertyValues("c15.caida-simulada.activa=false")
                .run(c -> assertThat(c).doesNotHaveBean(CaidaSimuladaController.class));
        contexto.withPropertyValues("c15.caida-simulada.activa=true")
                .run(c -> assertThat(c).hasSingleBean(CaidaSimuladaController.class));
    }
}
