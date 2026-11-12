package co.eci.c15.gameplay.application;

/** Avisa en tiempo real a los miembros de un equipo; nunca a otros equipos. */
public interface PosicionNotifier {
    void notificar(String matchId, String equipoId, PosicionJugadorDto posicion);
}
