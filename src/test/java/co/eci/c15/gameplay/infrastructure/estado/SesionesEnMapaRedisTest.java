package co.eci.c15.gameplay.infrastructure.estado;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SesionesEnMapaRedisTest extends RedisDePrueba {

    @Test
    void laUltimaConexionSeCompartePorRedisYCaduca() {
        SesionesEnMapaRedis replicaA = new SesionesEnMapaRedis(redis);
        SesionesEnMapaRedis replicaB = new SesionesEnMapaRedis(redis);

        assertTrue(replicaA.esLaUltima("m1", "ana", "ws-1"), "sin aviso de apertura, la que se cierra es la ultima");
        replicaA.abierta("m1", "ana", "ws-1");
        assertTrue(replicaB.esLaUltima("m1", "ana", "ws-1"));

        // Recargo: la apertura de la nueva la atiende una replica y el cierre de la vieja otra
        replicaA.abierta("m1", "ana", "ws-2");
        assertFalse(replicaB.esLaUltima("m1", "ana", "ws-1"), "volvio con otra conexion");
        assertTrue(replicaB.esLaUltima("m1", "ana", "ws-2"));

        Long vida = redis.getExpire(SesionesEnMapaRedis.clave("m1", "ana"));
        assertTrue(vida != null && vida > 0 && vida <= SesionesEnMapaRedis.VIDA.toSeconds(), "toda clave caduca");
    }
}
