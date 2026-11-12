package co.eci.c15.gameplay.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    public MapaIsometrico generar(String matchId) {
        if (matchId == null || matchId.isBlank()) throw new IllegalArgumentException("El id de la partida no puede estar vacio");
        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            MapaIsometrico mapa = construir(matchId, intento);
            if (mapa.todosAccesibles()) return mapa;
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
        int siguiente = 0;
        siguiente = agregar(componentes, celdas, siguiente, TipoComponente.ACERTIJO, "acertijo-", ACERTIJOS);
        siguiente = agregar(componentes, celdas, siguiente, TipoComponente.LLAVE, "llave-", LLAVES);
        siguiente = agregar(componentes, celdas, siguiente, TipoComponente.PUERTA, "puerta-", PUERTAS);
        agregar(componentes, celdas, siguiente, TipoComponente.OBSTACULO, "obstaculo-", OBSTACULOS);
        return new MapaIsometrico(matchId, ANCHO, ALTO, SPAWN, componentes);
    }

    private int agregar(List<ComponenteMapa> destino, List<Posicion> celdas, int desde,
                        TipoComponente tipo, String prefijo, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            destino.add(new ComponenteMapa(prefijo + (i + 1), tipo, celdas.get(desde + i)));
        }
        return desde + cantidad;
    }
}
