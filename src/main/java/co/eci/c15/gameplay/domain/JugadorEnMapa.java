package co.eci.c15.gameplay.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Personaje de un jugador sobre el mapa. El cambio de posicion es atomico: dos
 * movimientos simultaneos del mismo jugador se serializan y cada uno parte de la
 * posicion real en que quedo el anterior.
 */
public final class JugadorEnMapa {

    public record Movimiento(Posicion desde, Posicion hasta, List<Posicion> ruta) {}

    private final String matchId;
    private final String equipoId;
    private final String userId;
    private Posicion posicion;
    private String acertijoAbierto;

    public JugadorEnMapa(String matchId, String equipoId, String userId, Posicion posicionInicial) {
        this.matchId = Objects.requireNonNull(matchId);
        this.equipoId = Objects.requireNonNull(equipoId);
        this.userId = Objects.requireNonNull(userId);
        this.posicion = Objects.requireNonNull(posicionInicial);
    }

    /** Reconstruye al jugador tal como quedo guardado (estado compartido entre instancias). */
    public static JugadorEnMapa restaurar(String matchId, String equipoId, String userId, Posicion posicion,
                                          String acertijoAbierto) {
        JugadorEnMapa jugador = new JugadorEnMapa(matchId, equipoId, userId, posicion);
        jugador.acertijoAbierto = acertijoAbierto;
        return jugador;
    }

    public synchronized Movimiento mover(MapaIsometrico mapa, Posicion destino) {
        if (acertijoAbierto != null) throw new MovimientoInvalidoException("No puedes moverte mientras tienes abierto el acertijo " + acertijoAbierto);
        if (!mapa.dentro(destino)) throw new MovimientoInvalidoException("El destino esta fuera del mapa");
        if (!mapa.esTransitable(destino)) throw new MovimientoInvalidoException("El destino no es transitable");
        List<Posicion> ruta = mapa.rutaEntre(posicion, destino)
                .orElseThrow(() -> new MovimientoInvalidoException("No hay un camino hasta el destino"));
        Posicion desde = posicion;
        posicion = destino;
        return new Movimiento(desde, destino, ruta);
    }

    /**
     * Abre el acertijo: el jugador debe estar en una celda vecina y queda quieto hasta cerrarlo.
     * {@code bloquear} se ejecuta dentro del mismo paso atómico (si falla, no se abre).
     */
    public synchronized void abrirAcertijo(ComponenteMapa acertijo, Runnable bloquear) {
        if (acertijoAbierto != null && !acertijoAbierto.equals(acertijo.id())) {
            throw new OtroAcertijoAbiertoException(acertijoAbierto);
        }
        Posicion p = acertijo.posicion();
        if (Math.abs(p.x() - posicion.x()) + Math.abs(p.y() - posicion.y()) != 1) {
            throw new AcertijoLejosException(acertijo.id());
        }
        bloquear.run();
        acertijoAbierto = acertijo.id();
    }

    /** Cierra el acertijo si es el que tiene abierto; idempotente. */
    public synchronized void cerrarAcertijo(String componenteMapaId) {
        if (componenteMapaId.equals(acertijoAbierto)) acertijoAbierto = null;
    }

    public synchronized Optional<String> getAcertijoAbierto() { return Optional.ofNullable(acertijoAbierto); }

    public String getMatchId() { return matchId; }
    public String getEquipoId() { return equipoId; }
    public String getUserId() { return userId; }
    public synchronized Posicion getPosicion() { return posicion; }
}
