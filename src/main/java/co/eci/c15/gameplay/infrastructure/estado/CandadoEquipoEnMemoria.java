package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.application.CandadoEquipo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/** Con una sola instancia no hace falta: los objetos del dominio ya son atomicos en memoria. */
@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class CandadoEquipoEnMemoria implements CandadoEquipo {

    private final CandadoEquipo ninguno = CandadoEquipo.ninguno();

    @Override
    public <T> T ejecutar(String matchId, String ambito, Supplier<T> accion) {
        return ninguno.ejecutar(matchId, ambito, accion);
    }
}
