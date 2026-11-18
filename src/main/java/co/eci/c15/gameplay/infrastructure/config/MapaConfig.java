package co.eci.c15.gameplay.infrastructure.config;

import co.eci.c15.gameplay.domain.CatalogoAcertijos;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapaConfig {

    /** El mapa trae los objetos que piden los acertijos de la partida (ej. la lampara UV). */
    @Bean
    public GeneradorMapa generadorMapa(CatalogoAcertijos catalogo) {
        return new GeneradorMapa(catalogo::objetosDe);
    }
}
