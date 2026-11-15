package co.eci.c15;

import co.eci.c15.gameplay.infrastructure.web.DevErrorSimuladoController.ErrorSimuladoException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Prueba de diagnostico (O1): el error simulado sale como 500 y queda en el log con su excepcion. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ErrorSimuladoFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void elErrorSimuladoQuedaRegistradoComo500ConSuExcepcion(CapturedOutput salida) {
        ServletException e = assertThrows(ServletException.class, () -> mvc.perform(post("/dev/partidas/{m}/error", "m-o1")));

        assertInstanceOf(ErrorSimuladoException.class, e.getCause());
        assertTrue(salida.getAll().contains("POST /dev/partidas/{matchId}/error -> 500"), "la peticion queda registrada como 500");
        assertTrue(salida.getAll().contains("Error simulado para la prueba de diagnostico (O1) en la partida m-o1"), "con su excepcion");
    }
}
