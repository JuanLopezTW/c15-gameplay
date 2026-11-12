package co.eci.c15.gameplay.domain;

public interface AcertijoEnPartidaRepository {

    /** Devuelve el estado del acertijo para ese equipo en la partida, creándolo la primera vez. */
    AcertijoEnPartida obtener(String matchId, String equipoId, String componenteMapaId);

    void guardar(AcertijoEnPartida acertijo);
}
