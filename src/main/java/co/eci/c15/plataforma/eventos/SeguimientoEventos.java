package co.eci.c15.plataforma.eventos;

/**
 * Lo implementan las dos formas de mover los eventos entre servicios (en memoria y Kafka): las pruebas de
 * flujo lo usan para esperar a que los demas modulos reciban lo que se publico.
 */
public interface SeguimientoEventos {

    /** Si ya se entrego lo que esta instancia publico. */
    boolean entregados();
}
