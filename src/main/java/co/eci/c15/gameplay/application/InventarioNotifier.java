package co.eci.c15.gameplay.application;

/** Avisa en tiempo real a los miembros de un equipo su inventario actualizado; nunca a otros equipos. */
public interface InventarioNotifier {
    void notificar(InventarioDto inventario);
}
