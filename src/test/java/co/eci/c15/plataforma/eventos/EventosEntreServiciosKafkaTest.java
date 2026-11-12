package co.eci.c15.plataforma.eventos;

import co.eci.c15.common.events.SalaCerradaEvent;
import co.eci.c15.common.events.SesionEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventosEntreServiciosKafkaTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final EventosEntreServiciosKafka publicador = new EventosEntreServiciosKafka(kafka, eventos, "c15.");

    @AfterEach
    void limpiar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @SuppressWarnings("unchecked")
    private ProducerRecord<String, String> enviado() {
        ArgumentCaptor<ProducerRecord<String, String>> registro = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafka).send(registro.capture());
        return registro.getValue();
    }

    @Test
    void enviaAlTopicDelEventoConLaSalaComoLlaveYAlRecibirloLoReparte() {
        when(kafka.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<SendResult<String, String>>());

        publicador.publicar(new SesionEvent("CERRADA", "SALA", "s1", "ana", "ws-1"));
        ProducerRecord<String, String> registro = enviado();
        assertEquals("c15.sesion", registro.topic());
        assertEquals("s1", registro.key());
        assertEquals("{\"estado\":\"CERRADA\",\"ambito\":\"SALA\",\"id\":\"s1\",\"userId\":\"ana\",\"sessionId\":\"ws-1\"}", registro.value());
        assertFalse(publicador.entregados(), "falta recibirlo de vuelta");

        ConsumerRecord<String, String> recibido = new ConsumerRecord<>(registro.topic(), 0, 0, registro.key(), registro.value());
        registro.headers().forEach(h -> recibido.headers().add(h));
        publicador.recibir(recibido);

        verify(eventos).publishEvent(new SesionEvent("CERRADA", "SALA", "s1", "ana", "ws-1"));
        assertTrue(publicador.entregados());
    }

    @Test
    void conTransaccionSoloSaleDespuesDelCommitYSiSeDeshaceNoSale() {
        TransactionSynchronizationManager.initSynchronization();
        publicador.publicar(new SalaCerradaEvent("s1"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(kafka);
        TransactionSynchronizationManager.clearSynchronization();

        TransactionSynchronizationManager.initSynchronization();
        when(kafka.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<SendResult<String, String>>());
        publicador.publicar(new SalaCerradaEvent("s2"));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertEquals("s2", enviado().key());
    }

    @Test
    void loQueSoloSePublicaNoSeEsperaDeVuelta() {
        when(kafka.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<SendResult<String, String>>());

        publicador.publicar(new SalaCerradaEvent("s1"));

        assertEquals("c15.sala-cerrada", enviado().topic());
        assertTrue(publicador.entregados(), "este servicio no escucha sala-cerrada: no hay nada que esperar");
    }

    @Test
    void siKafkaNoRecibeElEnvioNoQuedaPendiente() {
        when(kafka.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("caido")));

        publicador.publicar(new SesionEvent("CERRADA", "SALA", "s1", "ana", "ws-1"));

        assertTrue(publicador.entregados());
    }

    @Test
    void ignoraTopicsAjenosYAceptaMensajesSinId() {
        publicador.recibir(new ConsumerRecord<>("otro.sala-cerrada", 0, 0, "s1", "{\"salaId\":\"s1\"}"));
        verifyNoInteractions(eventos);

        // sala-cerrada no la escucha este servicio
        publicador.recibir(new ConsumerRecord<>("c15.sala-cerrada", 0, 0, "s1", "{\"salaId\":\"s1\"}"));
        verifyNoInteractions(eventos);

        ConsumerRecord<String, String> sinId = new ConsumerRecord<>("c15.sesion", 0, 0, "s1", "{\"estado\":\"CERRADA\",\"ambito\":\"SALA\",\"id\":\"s1\",\"userId\":\"ana\",\"sessionId\":\"ws-1\"}");
        publicador.recibir(sinId);
        verify(eventos).publishEvent(new SesionEvent("CERRADA", "SALA", "s1", "ana", "ws-1"));
        assertNull(sinId.headers().lastHeader(EventosEntreServiciosKafka.HEADER_ID));
    }

    @Test
    void unJsonQueNoSePuedeLeerFallaSinReintentoYNoSePublica() {
        ConsumerRecord<String, String> roto = new ConsumerRecord<>("c15.sesion", 0, 0, "s1", "no es json");
        roto.headers().add(EventosEntreServiciosKafka.HEADER_ID, "x".getBytes(StandardCharsets.UTF_8));

        assertThrows(java.io.UncheckedIOException.class, () -> publicador.recibir(roto));
        verifyNoInteractions(eventos);
    }
}
