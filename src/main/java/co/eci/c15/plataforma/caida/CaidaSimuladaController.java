package co.eci.c15.plataforma.caida;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * Prueba de caida (escenario D1): la replica que atiende la peticion responde quien es y se muere en seco, como si
 * se fuera la luz (sin cerrar conexiones ni avisar a nadie). Azure la vuelve a levantar sola.
 * Solo existe si c15.caida-simulada.activa = true (Terraform la prende solo para la prueba) y, como todo /dev, solo
 * la puede usar el admin.
 */
@RestController
@ConditionalOnProperty(name = "c15.caida-simulada.activa", havingValue = "true")
public class CaidaSimuladaController {

    /** Codigo de salida de un proceso matado con SIGKILL. */
    static final int CODIGO_SALIDA = 137;
    /** Lo justo para que la respuesta alcance a salir. */
    private static final Duration ESPERA = Duration.ofMillis(300);
    private static final Logger log = LoggerFactory.getLogger(CaidaSimuladaController.class);

    private final String servicio;
    private final String replica;
    private final IntConsumer apagar;
    private final Duration espera;

    @Autowired
    public CaidaSimuladaController(@Value("${spring.application.name}") String servicio,
                                   @Value("${HOSTNAME:local}") String replica) {
        this(servicio, replica, CaidaSimuladaController::apagarEnSeco, ESPERA);
    }

    CaidaSimuladaController(String servicio, String replica, IntConsumer apagar, Duration espera) {
        this.servicio = servicio;
        this.replica = replica;
        this.apagar = apagar;
        this.espera = espera;
    }

    @PostMapping("/dev/caida/${spring.application.name}")
    public ResponseEntity<Map<String, String>> caer() {
        log.atWarn().addKeyValue("evento", "caida.simulada")
                .log("Caida simulada (D1): esta replica se apaga en seco");
        CompletableFuture.runAsync(() -> apagar.accept(CODIGO_SALIDA),
                CompletableFuture.delayedExecutor(espera.toMillis(), TimeUnit.MILLISECONDS));
        return ResponseEntity.accepted().body(Map.of("servicio", servicio, "replica", replica));
    }

    @SuppressWarnings("java:S1147") // es justo lo que se simula: un proceso que muere sin apagarse en orden
    private static void apagarEnSeco(int codigo) {
        Runtime.getRuntime().halt(codigo);
    }
}
