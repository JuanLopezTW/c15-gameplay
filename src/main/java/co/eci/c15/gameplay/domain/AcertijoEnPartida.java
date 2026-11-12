package co.eci.c15.gameplay.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Estado de un acertijo para un equipo dentro de una partida: si está resuelto y quién
 * lo tiene abierto. Solo un jugador del equipo puede tenerlo abierto a la vez.
 * Los cambios son atómicos (métodos sincronizados).
 */
public final class AcertijoEnPartida {

    public enum Estado { PENDIENTE, RESUELTO }

    public enum Resultado { CORRECTA, INCORRECTA }

    private final String matchId;
    private final String equipoId;
    private final String componenteMapaId;
    private Estado estado = Estado.PENDIENTE;
    private String poseedor;
    private Instant bloqueadoEn;
    private final List<String> entrada = new ArrayList<>();

    public AcertijoEnPartida(String matchId, String equipoId, String componenteMapaId) {
        this.matchId = Objects.requireNonNull(matchId);
        this.equipoId = Objects.requireNonNull(equipoId);
        this.componenteMapaId = Objects.requireNonNull(componenteMapaId);
    }

    /** Reconstruye el estado tal como quedo guardado (estado compartido entre instancias). */
    public static AcertijoEnPartida restaurar(String matchId, String equipoId, String componenteMapaId, Estado estado,
                                              String poseedor, Instant bloqueadoEn, List<String> entrada) {
        AcertijoEnPartida acertijo = new AcertijoEnPartida(matchId, equipoId, componenteMapaId);
        acertijo.estado = Objects.requireNonNull(estado);
        acertijo.poseedor = poseedor;
        acertijo.bloqueadoEn = bloqueadoEn;
        acertijo.entrada.addAll(entrada);
        return acertijo;
    }

    public synchronized Optional<Instant> getBloqueadoEn() { return Optional.ofNullable(bloqueadoEn); }

    /**
     * Bloquea el acertijo para el jugador. Se puede tomar si está libre, si ya era suyo, si el
     * bloqueo venció (ttl) o si quien lo tenía ya no sigue en la partida (se desconectó).
     *
     * @return el jugador al que se le quitó el bloqueo, si había otro
     * @throws AcertijoBloqueadoException si otro jugador lo tiene abierto
     */
    public synchronized Optional<String> bloquear(String userId, Instant ahora, Duration ttl,
                                                  Predicate<String> sigueEnPartida) {
        Objects.requireNonNull(userId);
        if (poseedor != null && !poseedor.equals(userId)
                && !vencido(ahora, ttl) && sigueEnPartida.test(poseedor)) {
            throw new AcertijoBloqueadoException(componenteMapaId);
        }
        String anterior = poseedor != null && !poseedor.equals(userId) ? poseedor : null;
        if (poseedor == null || anterior != null) entrada.clear();
        poseedor = userId;
        bloqueadoEn = ahora;
        return Optional.ofNullable(anterior);
    }

    /** Libera el bloqueo solo si lo tiene ese jugador; lo que habia ingresado se descarta. */
    public synchronized void liberar(String userId) {
        if (userId != null && userId.equals(poseedor)) {
            poseedor = null;
            bloqueadoEn = null;
            entrada.clear();
        }
    }

    /**
     * Registra un click del jugador que tiene el acertijo abierto. Verificar el bloqueo y
     * agregar la entrada es un solo paso atomico.
     *
     * @return la entrada acumulada tras este click
     */
    public synchronized List<String> registrarClick(String userId, String valor) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(valor);
        if (estado == Estado.RESUELTO) throw new AcertijoYaResueltoException(componenteMapaId);
        verificarPoseedor(userId);
        entrada.add(valor);
        return List.copyOf(entrada);
    }

    /** Borra lo ingresado hasta ahora; solo lo puede hacer quien tiene el acertijo abierto. */
    public synchronized void reiniciarEntrada(String userId) {
        if (estado == Estado.RESUELTO) throw new AcertijoYaResueltoException(componenteMapaId);
        verificarPoseedor(userId);
        entrada.clear();
    }

    /**
     * Evalua la solucion de quien tiene el acertijo abierto, en un solo paso atomico: si es
     * correcta el acertijo queda resuelto (y se libera); si no, se descarta lo ingresado para
     * que pueda intentarlo de nuevo. Con varios intentos simultaneos solo uno puede resolverlo.
     *
     * @param respuesta lo que envia el jugador; si es null se evalua lo ingresado con clicks
     */
    public synchronized Resultado intentarResolver(String userId, List<String> respuesta,
                                                   Predicate<List<String>> esCorrecta) {
        Objects.requireNonNull(userId);
        if (estado == Estado.RESUELTO) throw new AcertijoYaResueltoException(componenteMapaId);
        verificarPoseedor(userId);
        List<String> candidata = respuesta != null ? List.copyOf(respuesta) : List.copyOf(entrada);
        if (esCorrecta.test(candidata)) {
            marcarResuelto();
            return Resultado.CORRECTA;
        }
        entrada.clear();
        return Resultado.INCORRECTA;
    }

    /** Un companero que lo tiene abierto -> "en uso"; nadie lo tiene abierto -> primero hay que abrirlo. */
    private void verificarPoseedor(String userId) {
        if (poseedor != null && !poseedor.equals(userId)) throw new AcertijoBloqueadoException(componenteMapaId);
        if (poseedor == null) throw new AcertijoNoAbiertoException(componenteMapaId, userId);
    }

    public synchronized List<String> getEntrada() { return List.copyOf(entrada); }

    public synchronized void marcarResuelto() {
        estado = Estado.RESUELTO;
        poseedor = null;
        bloqueadoEn = null;
    }

    private boolean vencido(Instant ahora, Duration ttl) {
        return bloqueadoEn != null && !ahora.isBefore(bloqueadoEn.plus(ttl));
    }

    public synchronized boolean isResuelto() { return estado == Estado.RESUELTO; }
    public synchronized Estado getEstado() { return estado; }
    public synchronized Optional<String> getPoseedor() { return Optional.ofNullable(poseedor); }
    public String getMatchId() { return matchId; }
    public String getEquipoId() { return equipoId; }
    public String getComponenteMapaId() { return componenteMapaId; }
}
