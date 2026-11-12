package co.eci.c15.gameplay.application;

import java.util.function.Supplier;

/**
 * Serializa los cambios al estado de un equipo (o de toda la partida) entre instancias del back: leer el
 * estado, cambiarlo y guardarlo es un solo paso aunque dos jugadores del equipo actuen a la vez en
 * instancias distintas. Con el estado en memoria no hace falta: los objetos del dominio ya son atomicos.
 */
public interface CandadoEquipo {

    /** Ambito para lo que es de toda la partida (progreso), no de un equipo. */
    String PARTIDA = "*";

    <T> T ejecutar(String matchId, String ambito, Supplier<T> accion);

    default void ejecutar(String matchId, String ambito, Runnable accion) {
        ejecutar(matchId, ambito, () -> {
            accion.run();
            return null;
        });
    }

    /** Sin candado (una sola instancia, o pruebas). */
    static CandadoEquipo ninguno() {
        return new CandadoEquipo() {
            @Override
            public <T> T ejecutar(String matchId, String ambito, Supplier<T> accion) {
                return accion.get();
            }
        };
    }
}
