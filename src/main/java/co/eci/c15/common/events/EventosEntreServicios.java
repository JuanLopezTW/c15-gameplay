package co.eci.c15.common.events;

/**
 * Publica los eventos que cruzan servicios ({@link PartidaIniciadaEvent}, {@link MatchFinishedEvent},
 * {@link SalaCerradaEvent}). Quien escucha los recibe despues del commit y en otro hilo, como cuando
 * pasen por un broker al cortar en microservicios: por eso cada listener debe tolerar recibirlos tarde
 * o repetidos. Los eventos internos de un modulo siguen con {@code ApplicationEventPublisher}.
 */
public interface EventosEntreServicios {

    void publicar(Object evento);
}
