package co.eci.c15.common.events;

/**
 * Una conexion WebSocket (la atiende el broker) entro o salio del lobby de una sala o del mapa de una partida.
 * El broker publica ABIERTA cuando el front registra la sesion (SEND /app/salas/{id}/sesion o
 * /app/partidas/{m}/sesion) y la repite cada 30 s mientras siga abierta; CERRADA cuando se desconecta.
 *
 * @param estado    ABIERTA o CERRADA
 * @param ambito    SALA o PARTIDA
 * @param id        salaId o matchId (es la llave en Kafka: los de un mismo lobby llegan en orden)
 * @param userId    el jugador (sub del token)
 * @param sessionId la conexion: un jugador puede tener varias pestañas
 */
public record SesionEvent(String estado, String ambito, String id, String userId, String sessionId) {

    public static final String ABIERTA = "ABIERTA";
    public static final String CERRADA = "CERRADA";
    public static final String SALA = "SALA";
    public static final String PARTIDA = "PARTIDA";

    public boolean deSala() {
        return SALA.equals(ambito);
    }

    public boolean estaAbierta() {
        return ABIERTA.equals(estado);
    }
}
