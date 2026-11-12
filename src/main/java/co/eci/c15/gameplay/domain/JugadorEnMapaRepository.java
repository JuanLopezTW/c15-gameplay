package co.eci.c15.gameplay.domain;

import java.util.List;
import java.util.Optional;

public interface JugadorEnMapaRepository {
    Optional<JugadorEnMapa> find(String matchId, String userId);
    List<JugadorEnMapa> findByMatchId(String matchId);

    /** Registra al jugador solo si no estaba; devuelve el que quedo registrado. */
    JugadorEnMapa saveIfAbsent(JugadorEnMapa jugador);

    /** Guarda los cambios del jugador (posicion, acertijo abierto) si sigue en el mapa. */
    void guardar(JugadorEnMapa jugador);

    /** Saca al jugador del mapa; devuelve vacio si ya no estaba. */
    Optional<JugadorEnMapa> remove(String matchId, String userId);
}
