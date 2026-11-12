package co.eci.c15.gameplay.infrastructure.config;

import co.eci.c15.gameplay.domain.AsignadorRoles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
public class RolesConfig {

    @Bean
    public AsignadorRoles asignadorRoles() {
        return new AsignadorRoles(new SecureRandom());
    }
}
