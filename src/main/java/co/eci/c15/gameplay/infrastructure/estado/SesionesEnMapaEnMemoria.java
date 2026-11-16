package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.application.SesionesEnMapa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Con una sola replica (c15.estado.redis = false). */
@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class SesionesEnMapaEnMemoria implements SesionesEnMapa {

    private final Map<String, String> ultima = new ConcurrentHashMap<>();

    @Override
    public void abierta(String matchId, String userId, String sessionId) {
        ultima.put(matchId + "|" + userId, sessionId);
    }

    @Override
    public boolean esLaUltima(String matchId, String userId, String sessionId) {
        String actual = ultima.get(matchId + "|" + userId);
        return actual == null || Objects.equals(actual, sessionId);
    }
}
