package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import co.eci.c15.gameplay.application.MatchResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * Freezes the map while the challenge runs (HU-44.5) and once the match is over (HU-67.6):
 * moving, opening/solving puzzles and picking up objects answer 409. Closing a puzzle (DELETE)
 * is still allowed.
 */
public class MapFreezeInterceptor implements HandlerInterceptor {

    public static final String[] FROZEN_PATHS = {
            "/api/partidas/*/jugadores/*/movimiento",
            "/api/partidas/*/acertijos/**",
            "/api/partidas/*/objetos/**"
    };

    private final HeroVillainChallengeService challenges;
    private final MatchResultService results;
    private final ObjectMapper json;

    public MapFreezeInterceptor(HeroVillainChallengeService challenges, MatchResultService results, ObjectMapper json) {
        this.challenges = challenges;
        this.results = results;
        this.json = json;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equals(request.getMethod())) return true;
        String matchId = matchId(request);
        if (matchId == null) return true;
        if (results.isFinished(matchId)) return reject(response, "La partida ya termino");
        if (challenges.isActive(matchId)) return reject(response, "El mapa esta congelado mientras dura el reto Heroe vs Verdugo");
        return true;
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.CONFLICT.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(), Map.of("error", message));
        return false;
    }

    private static String matchId(HttpServletRequest request) {
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars instanceof Map<?, ?> map && map.get("matchId") instanceof String id) return id;
        String[] parts = request.getRequestURI().split("/");
        return parts.length > 3 ? parts[3] : null;
    }
}
