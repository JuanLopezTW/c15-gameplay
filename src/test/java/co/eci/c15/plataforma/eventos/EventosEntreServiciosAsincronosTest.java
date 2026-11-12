package co.eci.c15.plataforma.eventos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class EventosEntreServiciosAsincronosTest {

    private final List<Object> entregados = new CopyOnWriteArrayList<>();
    private final List<String> hilos = new CopyOnWriteArrayList<>();
    private final ApplicationEventPublisher eventos = evento -> {
        if ("revienta".equals(evento)) throw new IllegalStateException("un listener falla");
        hilos.add(Thread.currentThread().getName());
        entregados.add(evento);
    };
    private final EventosEntreServiciosAsincronos publicador = new EventosEntreServiciosAsincronos(eventos);

    @AfterEach
    void cerrar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
        publicador.destroy();
    }

    @Test
    void sinTransaccionSeEntregaEnOtroHiloYEnOrden() {
        publicador.publicar("inicio");
        publicador.publicar("fin");

        await().atMost(Duration.ofSeconds(2)).until(() -> entregados.size() == 2);
        assertEquals(List.of("inicio", "fin"), entregados);
        assertTrue(hilos.stream().allMatch(h -> h.startsWith("eventos-")), hilos.toString());
    }

    @Test
    void conTransaccionSoloSeEntregaDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();
        publicador.publicar("inicio");
        // Antes del commit no sale nada (durante todo el rato, no solo al final)
        await().during(Duration.ofMillis(200)).atMost(Duration.ofSeconds(2)).until(entregados::isEmpty);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        await().atMost(Duration.ofSeconds(2)).until(() -> entregados.equals(List.of("inicio")));
    }

    @Test
    void siLaTransaccionSeDeshaceNoSeEntrega() {
        TransactionSynchronizationManager.initSynchronization();
        publicador.publicar("inicio");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        await().during(Duration.ofMillis(200)).atMost(Duration.ofSeconds(2)).until(entregados::isEmpty);
    }

    @Test
    void unListenerQueFallaNoFrenaLosSiguientes() {
        publicador.publicar("revienta");
        publicador.publicar("despues");

        await().atMost(Duration.ofSeconds(2)).until(() -> entregados.equals(List.of("despues")));
    }
}
