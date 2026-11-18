package co.eci.c15.gameplay.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Genera el mapa de una partida. Es determinista: el mismo matchId produce siempre
 * el mismo mapa, asi todos los jugadores ven exactamente lo mismo.
 */
public class GeneradorMapa {

    static final int ANCHO = 12;
    static final int ALTO = 12;
    static final int ACERTIJOS = 3;
    static final int LLAVES = 2;
    static final int PUERTAS = 1;
    static final int OBSTACULOS = 6;
    static final int MAX_INTENTOS = 50;

    private static final Posicion SPAWN = new Posicion(0, 0);

    private final Function<String, List<String>> objetosDePartida;

    public GeneradorMapa() {
        this(matchId -> List.of());
    }

    /** @param objetosDePartida objetos que los acertijos de la partida necesitan en el mapa (componentes OBJETO) */
    public GeneradorMapa(Function<String, List<String>> objetosDePartida) {
        this.objetosDePartida = objetosDePartida;
    }

    public MapaIsometrico generar(String matchId) {
        if (matchId == null || matchId.isBlank()) throw new IllegalArgumentException("El id de la partida no puede estar vacio");
        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            MapaIsometrico mapa = construir(matchId, intento);
            if (mapa != null && mapa.todosAccesibles()) return mapa;
        }
        throw new MapaNoGeneradoException("No se pudo generar un mapa valido para la partida " + matchId);
    }

    private MapaIsometrico construir(String matchId, int intento) {
        Random azar = new Random(matchId.hashCode() * 31L + intento);
        List<Posicion> celdas = new ArrayList<>();
        for (int x = 0; x < ANCHO; x++) {
            for (int y = 0; y < ALTO; y++) {
                Posicion p = new Posicion(x, y);
                if (!p.equals(SPAWN)) celdas.add(p);
            }
        }
        Collections.shuffle(celdas, azar);

        List<ComponenteMapa> componentes = new ArrayList<>();
        boolean cupo = agregar(componentes, celdas, TipoComponente.ACERTIJO, "acertijo-", ACERTIJOS)
                && agregar(componentes, celdas, TipoComponente.LLAVE, "llave-", LLAVES)
                && agregar(componentes, celdas, TipoComponente.PUERTA, "puerta-", PUERTAS);
        for (String objeto : objetosDePartida.apply(matchId)) {
            cupo = cupo && agregar(componentes, celdas, TipoComponente.OBJETO, objeto);
        }
        cupo = cupo && agregar(componentes, celdas, TipoComponente.OBSTACULO, "obstaculo-", OBSTACULOS);
        return cupo ? new MapaIsometrico(matchId, ANCHO, ALTO, SPAWN, componentes) : null;
    }

    private boolean agregar(List<ComponenteMapa> destino, List<Posicion> celdas,
                            TipoComponente tipo, String prefijo, int cantidad) {
        for (int i = 1; i <= cantidad; i++) {
            if (!agregar(destino, celdas, tipo, prefijo + i)) return false;
        }
        return true;
    }

    /** Pone el componente en la primera celda libre (en el orden barajado) que cumpla {@link #puedeIr}. */
    private boolean agregar(List<ComponenteMapa> destino, List<Posicion> celdas, TipoComponente tipo, String id) {
        for (int i = 0; i < celdas.size(); i++) {
            Posicion p = celdas.get(i);
            if (puedeIr(tipo, p, destino)) {
                destino.add(new ComponenteMapa(id, tipo, p));
                celdas.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Lo que se usa con E (acertijo, llave, puerta, objeto) no puede quedar pegado a ningun otro componente,
     * ni en diagonal: queda suelto, se ve en su casilla y se llega sin confundirlo con lo de al lado.
     * Los obstaculos si pueden quedar juntos entre ellos.
     */
    static boolean puedeIr(TipoComponente tipo, Posicion p, List<ComponenteMapa> puestos) {
        boolean interactivo = tipo != TipoComponente.OBSTACULO;
        for (ComponenteMapa otro : puestos) {
            boolean pegados = Math.abs(otro.posicion().x() - p.x()) <= 1 && Math.abs(otro.posicion().y() - p.y()) <= 1;
            if (pegados && (interactivo || otro.tipo() != TipoComponente.OBSTACULO)) return false;
        }
        return true;
    }
}
