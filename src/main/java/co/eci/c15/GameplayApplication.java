package co.eci.c15;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de gameplay del EscapeRoom C15: mapa, acertijos, inventario, roles, cronometro, reto y resultado. Sale del monolito
 * (repo ScapeRoomC15) con los mismos paquetes; lo comun a todos los servicios esta en {@code plataforma}.
 */
@SpringBootApplication
public class GameplayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameplayApplication.class, args);
    }
}
