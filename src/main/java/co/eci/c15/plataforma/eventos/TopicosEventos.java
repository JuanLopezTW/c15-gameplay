package co.eci.c15.plataforma.eventos;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.common.events.SalaCerradaEvent;
import co.eci.c15.common.events.SesionEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * El contrato de los eventos entre servicios en Kafka: un topic por evento, la llave es la partida o la
 * sala (asi los de una misma partida quedan en la misma particion y en orden) y el valor es el JSON del
 * record. Cada servicio tiene su propia copia del record con los mismos campos.
 */
final class TopicosEventos {

    private static final String LLAVE_PARTIDA = "matchId";

    /** Un evento que cruza servicios: su topic (sin prefijo), su clase y de donde sale la llave. */
    record Topico(String nombre, Class<?> tipo, Function<Object, String> llave, String campoLog) {
    }

    static final List<Topico> TODOS = List.of(
            new Topico("partida-iniciada", PartidaIniciadaEvent.class,
                    e -> ((PartidaIniciadaEvent) e).matchId(), LLAVE_PARTIDA),
            new Topico("partida-terminada", MatchFinishedEvent.class,
                    e -> ((MatchFinishedEvent) e).matchId(), LLAVE_PARTIDA),
            new Topico("sala-cerrada", SalaCerradaEvent.class,
                    e -> ((SalaCerradaEvent) e).salaId(), "salaId"),
            new Topico("sesion", SesionEvent.class,
                    e -> ((SesionEvent) e).id(), LLAVE_PARTIDA));

    /** Los que escucha este servicio: el inicio (arma la partida), su propio fin (libera estado) y las sesiones del mapa. */
    static final List<String> ESCUCHADOS = List.of("partida-iniciada", "partida-terminada", "sesion");

    static boolean escuchado(Topico topico) {
        return ESCUCHADOS.contains(topico.nombre());
    }

    private TopicosEventos() {
    }

    /**
     * Como se escribe y lee el JSON, fijo aqui para no depender de la configuracion de cada servicio:
     * duraciones en ISO-8601 ("PT10M") y los campos que no se conocen se ignoran (un servicio puede
     * agregar campos sin romper a los demas).
     */
    static ObjectMapper json() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    static Topico de(Class<?> tipo) {
        return TODOS.stream().filter(t -> t.tipo().equals(tipo)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(tipo.getSimpleName() + " no es un evento entre servicios"));
    }

    /** El topic escuchado con ese nombre (con prefijo); vacio si no es de los que escucha este servicio. */
    static Optional<Topico> porNombre(String prefijo, String topicConPrefijo) {
        return TODOS.stream().filter(t -> escuchado(t) && (prefijo + t.nombre()).equals(topicConPrefijo)).findFirst();
    }
}
