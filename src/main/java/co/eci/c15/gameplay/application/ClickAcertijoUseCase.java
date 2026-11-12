package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.AcertijoEnPartidaRepository;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.ElementoInteractivoRepository;
import co.eci.c15.gameplay.domain.ElementoNoValidoException;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import org.springframework.stereotype.Service;

/**
 * Interaccion por clicks con un acertijo abierto (HU-28.5). Cada click sobre un elemento
 * interactivo se agrega, en orden, a lo que el equipo lleva ingresado. Solo el jugador que
 * tiene el acertijo abierto (el bloqueo de HU-35.5) puede hacer click.
 */
@Service
public class ClickAcertijoUseCase {

    static final int MAX_LONGITUD_VALOR = 50;

    private final JugadorEnMapaRepository jugadores;
    private final AcertijoRepository acertijos;
    private final ElementoInteractivoRepository elementos;
    private final AcertijoEnPartidaRepository estados;
    private final CandadoEquipo candado;

    public ClickAcertijoUseCase(JugadorEnMapaRepository jugadores, AcertijoRepository acertijos,
                                ElementoInteractivoRepository elementos, AcertijoEnPartidaRepository estados,
                                CandadoEquipo candado) {
        this.jugadores = jugadores;
        this.acertijos = acertijos;
        this.elementos = elementos;
        this.estados = estados;
        this.candado = candado;
    }

    /**
     * @param valor lo que se eligio con el click (ej. un color); si no viene, cuenta el id del elemento
     */
    public EntradaAcertijoDto click(String matchId, String componenteMapaId, String userId,
                                    String elementoId, String valor) {
        JugadorEnMapa jugador = jugadorDe(matchId, userId);
        Acertijo acertijo = acertijos.findByComponenteMapaId(componenteMapaId)
                .orElseThrow(() -> new ComponenteSinAcertijoException(componenteMapaId));
        if (elementoId == null || elementos.findByAcertijoId(acertijo.getId()).stream()
                .noneMatch(e -> e.getId().equals(elementoId))) {
            throw new ElementoNoValidoException(componenteMapaId, elementoId);
        }
        String token = valor == null || valor.isBlank() ? elementoId : valor.trim();
        if (token.length() > MAX_LONGITUD_VALOR) {
            throw new IllegalArgumentException("El valor del click no puede superar " + MAX_LONGITUD_VALOR + " caracteres");
        }
        return candado.ejecutar(matchId, jugador.getEquipoId(), () -> {
            AcertijoEnPartida estado = estados.obtener(matchId, jugador.getEquipoId(), componenteMapaId);
            estado.registrarClick(userId, token);
            estados.guardar(estado);
            return EntradaAcertijoDto.from(estado);
        });
    }

    /** Borra lo ingresado para empezar de nuevo; solo quien lo tiene abierto. */
    public EntradaAcertijoDto reiniciar(String matchId, String componenteMapaId, String userId) {
        JugadorEnMapa jugador = jugadorDe(matchId, userId);
        return candado.ejecutar(matchId, jugador.getEquipoId(), () -> {
            AcertijoEnPartida estado = estados.obtener(matchId, jugador.getEquipoId(), componenteMapaId);
            estado.reiniciarEntrada(userId);
            estados.guardar(estado);
            return EntradaAcertijoDto.from(estado);
        });
    }

    /** Lo que el equipo del jugador lleva ingresado en el acertijo. */
    public EntradaAcertijoDto consultar(String matchId, String componenteMapaId, String userId) {
        JugadorEnMapa jugador = jugadorDe(matchId, userId);
        acertijos.findByComponenteMapaId(componenteMapaId)
                .orElseThrow(() -> new ComponenteSinAcertijoException(componenteMapaId));
        return EntradaAcertijoDto.from(estados.obtener(matchId, jugador.getEquipoId(), componenteMapaId));
    }

    private JugadorEnMapa jugadorDe(String matchId, String userId) {
        return jugadores.find(matchId, userId).orElseThrow(() -> new JugadorNoRegistradoException(matchId, userId));
    }
}
