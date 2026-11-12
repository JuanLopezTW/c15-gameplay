package co.eci.c15;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La seguridad real, sin el ayudante de tokens de los otros flujos: el jugador sale del token y de nada mas. */
@SpringBootTest
class SeguridadFlujoTest {

    @Autowired
    private WebApplicationContext contexto;

    /** Sin esto el primer token falso iria a buscar las llaves del proveedor por la red. */
    @MockitoBean
    private JwtDecoder tokens;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(tokens.decode("token-falso")).thenThrow(new BadJwtException("firma invalida"));
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private static RequestPostProcessor como(String sub) {
        return jwt().jwt(token -> token.subject(sub));
    }

    @Test
    void sinTokenOConTokenInvalidoLaApiNoResponde() throws Exception {
        mvc.perform(get("/api/partidas/m1/cronometro")).andExpect(status().isUnauthorized());
        mvc.perform(get("/dev/partidas/m1/inicio")).andExpect(status().isUnauthorized());
        // Escribir sin token ni siquiera pasa CSRF
        mvc.perform(post("/api/partidas/m1/reto/clicks").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/partidas/m1/mapa").header("Authorization", "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laVersionNoPideToken() throws Exception {
        mvc.perform(get("/api/version")).andExpect(status().isOk());
    }

    @Test
    void nadiePuedeMoverNiDesconectarAOtroEnLaPartida() throws Exception {
        mvc.perform(post("/api/partidas/m1/jugadores/{u}/movimiento", "auth0|ana").with(como("auth0|beto"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"x\":1,\"y\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("propio")));
        mvc.perform(delete("/api/partidas/m1/jugadores/{u}", "auth0|ana").with(como("auth0|beto")))
                .andExpect(status().isForbidden());
    }
}
