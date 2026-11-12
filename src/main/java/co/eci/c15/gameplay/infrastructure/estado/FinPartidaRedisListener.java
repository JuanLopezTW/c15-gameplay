package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.common.events.MatchFinishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Al terminar una partida (lo ve solo la instancia donde termino) se avisa por Redis pub/sub a todas las
 * instancias para que borren su copia local del mapa, y lo que queda de la partida en Redis pasa a vivir
 * solo {@link #DESPUES_DEL_FIN}: la regla es que Redis guarde poco y por poco tiempo.
 */
@Configuration
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
public class FinPartidaRedisListener {

    private static final Logger log = LoggerFactory.getLogger(FinPartidaRedisListener.class);

    static final String CANAL = "c15:eventos:partida-terminada";
    static final Duration DESPUES_DEL_FIN = Duration.ofMinutes(5);

    private final EstadoPartidaRedis estado;
    private final MapaRedis mapas;

    public FinPartidaRedisListener(EstadoPartidaRedis estado, MapaRedis mapas) {
        this.estado = estado;
        this.mapas = mapas;
    }

    @EventListener
    public void onPartidaTerminada(MatchFinishedEvent evento) {
        estado.acortar(evento.matchId(), DESPUES_DEL_FIN);
        estado.redis().convertAndSend(CANAL, evento.matchId());
    }

    /** Escucha el aviso de cualquier instancia (tambien el propio). */
    @Bean
    public RedisMessageListenerContainer avisosPartidaTerminada(RedisConnectionFactory conexion) {
        RedisMessageListenerContainer contenedor = new RedisMessageListenerContainer();
        contenedor.setConnectionFactory(conexion);
        contenedor.addMessageListener((mensaje, patron) -> {
            String matchId = new String(mensaje.getBody(), StandardCharsets.UTF_8);
            mapas.olvidar(matchId);
            log.debug("Copia local del mapa de {} borrada", matchId);
        }, new ChannelTopic(CANAL));
        return contenedor;
    }
}
