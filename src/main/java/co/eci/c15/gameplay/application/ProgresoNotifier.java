package co.eci.c15.gameplay.application;

/** Avisa en tiempo real a todos los equipos de la partida el progreso de cada uno. */
public interface ProgresoNotifier {
    void notificar(ProgresoPartidaDto progreso);
}
