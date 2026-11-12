package co.eci.c15.gameplay.application;

/**
 * Quien le da cuerda a cada partida: solo el dueño manda los avisos periodicos (cronometro, marcador del
 * reto) y decide cuando arranca el reto o se acaba el tiempo, asi no salen repetidos desde cada instancia.
 * Si el dueño muere, otra instancia toma la partida y sigue desde el estado guardado.
 */
public interface DuenoPartidas {

    /** Si esta instancia es (o acaba de convertirse en) la dueña de la partida; renueva el turno. */
    boolean esMia(String matchId);
}
