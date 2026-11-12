package co.eci.c15.plataforma.eventos;

import co.eci.c15.common.events.EventosEntreServicios;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Los eventos entre servicios por Kafka. Se envian despues del commit de quien los publica (si se deshace, no
 * salen) y los que escucha este servicio se reparten a sus listeners con {@link ApplicationEventPublisher}.
 * Las replicas de este servicio estan en el mismo grupo: cada evento lo atiende una sola. Kafka puede
 * entregar un evento repetido: los listeners son idempotentes.
 */
@Component
@ConditionalOnProperty(name = "c15.eventos.kafka", havingValue = "true")
public class EventosEntreServiciosKafka implements EventosEntreServicios, SeguimientoEventos {

    static final String HEADER_ID = "c15-evento-id";

    private static final Logger log = LoggerFactory.getLogger(EventosEntreServiciosKafka.class);

    private final KafkaTemplate<String, String> kafka;
    private final ApplicationEventPublisher eventos;
    private final ObjectMapper json = TopicosEventos.json();
    private final String prefijo;
    /** Lo que esta instancia envio y aun no recibe de vuelta (solo para que las pruebas esperen). */
    private final Set<String> pendientes = ConcurrentHashMap.newKeySet();

    public EventosEntreServiciosKafka(KafkaTemplate<String, String> kafka, ApplicationEventPublisher eventos,
                                      @Value("${c15.eventos.prefijo:c15.}") String prefijo) {
        this.kafka = kafka;
        this.eventos = eventos;
        this.prefijo = prefijo;
    }

    @Override
    public void publicar(Object evento) {
        TopicosEventos.Topico topico = TopicosEventos.de(evento.getClass());
        String valor = aJson(evento);
        Runnable enviar = () -> enviar(topico, evento, valor);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar.run();
                }
            });
        } else {
            enviar.run();
        }
    }

    private void enviar(TopicosEventos.Topico topico, Object evento, String valor) {
        String id = UUID.randomUUID().toString();
        ProducerRecord<String, String> registro = new ProducerRecord<>(prefijo + topico.nombre(), topico.llave().apply(evento), valor);
        registro.headers().add(HEADER_ID, id.getBytes(StandardCharsets.UTF_8));
        // Solo se espera de vuelta lo que este servicio tambien escucha
        if (TopicosEventos.escuchado(topico)) pendientes.add(id);
        kafka.send(registro).whenComplete((ok, error) -> {
            if (error != null) {
                pendientes.remove(id);
                log.error("No se pudo enviar {} a Kafka", evento.getClass().getSimpleName(), error);
            }
        });
    }

    @KafkaListener(topics = "#{T(co.eci.c15.plataforma.eventos.KafkaEventosConfig).topics('${c15.eventos.prefijo:c15.}')}",
            groupId = "${c15.eventos.grupo:c15-gameplay}")
    void recibir(ConsumerRecord<String, String> registro) {
        TopicosEventos.Topico topico = TopicosEventos.porNombre(prefijo, registro.topic()).orElse(null);
        if (topico == null) return;
        MDC.put(topico.campoLog(), registro.key());
        try {
            // Si un listener falla, el error sube y Kafka lo reintenta (ver KafkaEventosConfig)
            eventos.publishEvent(deJson(registro.value(), topico.tipo()));
        } finally {
            MDC.remove(topico.campoLog());
            Header id = registro.headers().lastHeader(HEADER_ID);
            if (id != null) pendientes.remove(new String(id.value(), StandardCharsets.UTF_8));
        }
    }

    @Override
    public boolean entregados() {
        return pendientes.isEmpty();
    }

    private String aJson(Object evento) {
        try {
            return json.writeValueAsString(evento);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Object deJson(String valor, Class<?> tipo) {
        try {
            return json.readValue(valor, tipo);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
