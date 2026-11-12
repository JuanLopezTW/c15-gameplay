package co.eci.c15.gameplay.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Optional;

/** Final result of the Hero vs Executioner challenge. {@code winnerTeamId} is null on a tie. */
public record ChallengeResult(String matchId, String winnerTeamId, Map<String, Long> scores) {

    public ChallengeResult {
        scores = Map.copyOf(scores);
    }

    /**
     * Cierra el marcador: gana el equipo con mas puntos; un empate en el primer lugar no tiene ganador.
     * Los puntajes quedan ordenados por id de equipo.
     */
    public static ChallengeResult deMarcador(String matchId, Map<String, Long> puntajes) {
        Map<String, Long> ordenados = new TreeMap<>(puntajes);
        long mejor = Collections.max(ordenados.values());
        List<String> lideres = ordenados.entrySet().stream()
                .filter(e -> e.getValue() == mejor).map(Map.Entry::getKey).toList();
        return new ChallengeResult(matchId, lideres.size() == 1 ? lideres.get(0) : null, ordenados);
    }

    public Optional<String> winner() {
        return Optional.ofNullable(winnerTeamId);
    }
}
