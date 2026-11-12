package co.eci.c15.gameplay.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Mapa de una partida: grilla ancho x alto con componentes fijos. Es inmutable,
 * por lo que se puede compartir entre hilos sin sincronizacion.
 */
public final class MapaIsometrico {

    private static final int[][] VECINOS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final String matchId;
    private final int ancho;
    private final int alto;
    private final Posicion spawn;
    private final List<ComponenteMapa> componentes;
    private final Set<Posicion> ocupadas = new HashSet<>();

    public MapaIsometrico(String matchId, int ancho, int alto, Posicion spawn, List<ComponenteMapa> componentes) {
        if (matchId == null || matchId.isBlank()) throw new IllegalArgumentException("El id de la partida no puede estar vacio");
        if (ancho < 1 || alto < 1) throw new IllegalArgumentException("Las dimensiones del mapa deben ser positivas");
        this.matchId = matchId;
        this.ancho = ancho;
        this.alto = alto;
        this.spawn = spawn;
        if (!dentro(spawn)) throw new IllegalArgumentException("El punto de aparicion esta fuera del mapa");
        this.componentes = List.copyOf(componentes);
        Set<String> ids = new HashSet<>();
        for (ComponenteMapa c : this.componentes) {
            if (!ids.add(c.id())) throw new IllegalArgumentException("Componente repetido: " + c.id());
            if (!dentro(c.posicion())) throw new IllegalArgumentException("Componente fuera del mapa: " + c.id());
            if (c.posicion().equals(spawn)) throw new IllegalArgumentException("Componente sobre el punto de aparicion: " + c.id());
            if (!ocupadas.add(c.posicion())) throw new IllegalArgumentException("Dos componentes en la misma celda: " + c.posicion());
        }
    }

    public boolean dentro(Posicion p) {
        return p != null && p.x() >= 0 && p.x() < ancho && p.y() >= 0 && p.y() < alto;
    }

    public boolean esTransitable(Posicion p) {
        return dentro(p) && !ocupadas.contains(p);
    }

    /**
     * Ruta mas corta (movimientos de a una celda, sin diagonales) entre dos celdas transitables.
     * No incluye el origen e incluye el destino; si ya esta en el destino la ruta es vacia.
     */
    public Optional<List<Posicion>> rutaEntre(Posicion origen, Posicion destino) {
        if (!esTransitable(origen) || !esTransitable(destino)) return Optional.empty();
        if (origen.equals(destino)) return Optional.of(List.of());
        Map<Posicion, Posicion> previo = recorrer(origen);
        if (!previo.containsKey(destino)) return Optional.empty();
        List<Posicion> ruta = new ArrayList<>();
        for (Posicion p = destino; !p.equals(origen); p = previo.get(p)) ruta.add(p);
        Collections.reverse(ruta);
        return Optional.of(ruta);
    }

    /** Todo componente tiene al menos una celda vecina alcanzable desde el spawn. */
    public boolean todosAccesibles() {
        Set<Posicion> alcanzables = recorrer(spawn).keySet();
        for (ComponenteMapa c : componentes) {
            boolean accesible = false;
            for (int[] d : VECINOS) {
                if (alcanzables.contains(new Posicion(c.posicion().x() + d[0], c.posicion().y() + d[1]))) {
                    accesible = true;
                    break;
                }
            }
            if (!accesible) return false;
        }
        return true;
    }

    private Map<Posicion, Posicion> recorrer(Posicion origen) {
        Map<Posicion, Posicion> previo = new HashMap<>();
        previo.put(origen, origen);
        Queue<Posicion> cola = new ArrayDeque<>();
        cola.add(origen);
        while (!cola.isEmpty()) {
            Posicion actual = cola.poll();
            for (int[] d : VECINOS) {
                Posicion vecino = new Posicion(actual.x() + d[0], actual.y() + d[1]);
                if (esTransitable(vecino) && !previo.containsKey(vecino)) {
                    previo.put(vecino, actual);
                    cola.add(vecino);
                }
            }
        }
        return previo;
    }

    public String getMatchId() { return matchId; }
    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }
    public Posicion getSpawn() { return spawn; }
    public List<ComponenteMapa> getComponentes() { return componentes; }
}
