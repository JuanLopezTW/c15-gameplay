package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.Posicion;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinPartidaRedisListenerTest extends RedisDePrueba {

    @Test
    void alTerminarLaPartidaTodasLasInstanciasOlvidanElMapaYLoDemasDuraPocosMinutos() throws Exception {
        MapaRedis instanciaA = new MapaRedis(estado);
        MapaRedis instanciaB = new MapaRedis(estado);
        instanciaA.saveIfAbsent(new GeneradorMapa().generar("m1"));
        instanciaB.findByMatchId("m1");
        new JugadorEnMapaRedis(estado).saveIfAbsent(new JugadorEnMapa("m1", "e1", "ana", new Posicion(1, 1)));

        FinPartidaRedisListener enA = new FinPartidaRedisListener(estado, instanciaA);
        FinPartidaRedisListener enB = new FinPartidaRedisListener(estado, instanciaB);
        RedisMessageListenerContainer escuchaA = enA.avisosPartidaTerminada(conexion);
        RedisMessageListenerContainer escuchaB = enB.avisosPartidaTerminada(conexion);
        for (RedisMessageListenerContainer c : new RedisMessageListenerContainer[]{escuchaA, escuchaB}) {
            c.afterPropertiesSet();
            c.start();
        }
        try {
            Thread.sleep(300); // que se suscriban
            enA.onPartidaTerminada(new MatchFinishedEvent("m1", "TIME_UP", "e1", Map.of("e1", 1), Map.of()));

            for (int i = 0; i < 50 && instanciaB.tieneCopia("m1"); i++) Thread.sleep(50);
            assertFalse(instanciaA.tieneCopia("m1"));
            assertFalse(instanciaB.tieneCopia("m1"), "la otra instancia recibio el aviso");
            long vida = segundosDeVida(EstadoPartidaRedis.clave("m1", "jugadores"));
            assertTrue(vida > 0 && vida <= FinPartidaRedisListener.DESPUES_DEL_FIN.toSeconds(), "caduca en " + vida);
        } finally {
            escuchaA.destroy();
            escuchaB.destroy();
        }
    }
}
