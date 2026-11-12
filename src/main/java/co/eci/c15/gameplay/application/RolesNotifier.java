package co.eci.c15.gameplay.application;

/** Avisa a un equipo los roles recien asignados (el frontend muestra el pop-up del rol). */
public interface RolesNotifier {
    void notificar(AsignacionRolesDto asignacion);
}
