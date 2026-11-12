package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.SesionEvent;
import co.eci.c15.gameplay.application.DesconectarJugadorUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class SesionesPartidaListenerTest {

    private final DesconectarJugadorUseCase desconectar = mock(DesconectarJugadorUseCase.class);
    private final SesionesPartidaListener listener = new SesionesPartidaListener(desconectar);

    @Test
    void alCerrarseLaConexionDelMapaElJugadorSaleDeLaPartida() {
        listener.onSesion(new SesionEvent(SesionEvent.CERRADA, SesionEvent.PARTIDA, "m1", "ana", "ws-1"));

        verify(desconectar).ejecutar("m1", "ana");
    }

    @Test
    void abrirORenovarLaConexionNoHaceNada() {
        listener.onSesion(new SesionEvent(SesionEvent.ABIERTA, SesionEvent.PARTIDA, "m1", "ana", "ws-1"));

        verifyNoInteractions(desconectar);
    }

    @Test
    void lasSesionesDelLobbyNoSonDeGameplay() {
        listener.onSesion(new SesionEvent(SesionEvent.CERRADA, SesionEvent.SALA, "s1", "ana", "ws-1"));

        verifyNoInteractions(desconectar);
    }
}
