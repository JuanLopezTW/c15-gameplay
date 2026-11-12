package co.eci.c15.gameplay.infrastructure.config;

import co.eci.c15.gameplay.domain.GeneradorMapa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapaConfig {

    @Bean
    public GeneradorMapa generadorMapa() {
        return new GeneradorMapa();
    }
}
