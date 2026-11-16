package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.SesionEvent;
import co.eci.c15.gameplay.application.DesconectarJugadorUseCase;
import co.eci.c15.gameplay.infrastructure.estado.SesionesEnMapaEnMemoria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SesionesPartidaListenerTest {

    private static final Duration GRACIA = Duration.ofMillis(150);

    private final DesconectarJugadorUseCase desconectar = mock(DesconectarJugadorUseCase.class);
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    private final SesionesPartidaListener listener;

    SesionesPartidaListenerTest() {
        scheduler.initialize();
        listener = new SesionesPartidaListener(desconectar, new SesionesEnMapaEnMemoria(), scheduler, GRACIA);
    }

    @AfterEach
    void apagar() {
        scheduler.shutdown();
    }

    private static SesionEvent mapa(String estado, String sessionId) {
        return new SesionEvent(estado, SesionEvent.PARTIDA, "m1", "ana", sessionId);
    }

    @Test
    void siNoVuelveDentroDeLaGraciaSaleDeLaPartida() {
        listener.onSesion(mapa(SesionEvent.ABIERTA, "ws-1"));
        listener.onSesion(mapa(SesionEvent.CERRADA, "ws-1"));

        verify(desconectar, after(50).never()).ejecutar("m1", "ana");
        verify(desconectar, timeout(2000)).ejecutar("m1", "ana");
    }

    @Test
    void siRecargaYAbreOtraConexionSigueEnLaPartida() {
        listener.onSesion(mapa(SesionEvent.ABIERTA, "ws-1"));
        listener.onSesion(mapa(SesionEvent.CERRADA, "ws-1"));
        listener.onSesion(mapa(SesionEvent.ABIERTA, "ws-2"));

        verify(desconectar, after(GRACIA.toMillis() * 4).never()).ejecutar(anyString(), anyString());
    }

    @Test
    void laConexionNuevaPuedeAvisarAntesQueElCierreDeLaVieja() {
        listener.onSesion(mapa(SesionEvent.ABIERTA, "ws-1"));
        listener.onSesion(mapa(SesionEvent.ABIERTA, "ws-2"));
        listener.onSesion(mapa(SesionEvent.CERRADA, "ws-1"));

        verify(desconectar, after(GRACIA.toMillis() * 4).never()).ejecutar(anyString(), anyString());
    }

    @Test
    void sinNingunAvisoDeAperturaIgualSaleAlCerrarse() {
        listener.onSesion(mapa(SesionEvent.CERRADA, "ws-1"));

        verify(desconectar, timeout(2000)).ejecutar("m1", "ana");
    }

    @Test
    void unErrorAlRevisarNoTumbaElScheduler() {
        doThrow(new IllegalStateException("redis caido")).when(desconectar).ejecutar("m1", "ana");
        listener.onSesion(mapa(SesionEvent.CERRADA, "ws-1"));
        verify(desconectar, timeout(2000)).ejecutar("m1", "ana");

        listener.onSesion(new SesionEvent(SesionEvent.CERRADA, SesionEvent.PARTIDA, "m2", "beto", "ws-9"));
        verify(desconectar, timeout(2000)).ejecutar("m2", "beto");
    }

    @Test
    void lasSesionesDelLobbyNoSonDeGameplay() {
        listener.onSesion(new SesionEvent(SesionEvent.CERRADA, SesionEvent.SALA, "s1", "ana", "ws-1"));

        verify(desconectar, after(GRACIA.toMillis() * 3).never()).ejecutar(anyString(), anyString());
    }
}
