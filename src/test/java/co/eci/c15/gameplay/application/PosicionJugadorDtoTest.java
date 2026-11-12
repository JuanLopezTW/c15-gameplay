package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Posicion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PosicionJugadorDtoTest {

    @Test
    void movimientoLlevaLaRutaEnElJson() throws Exception {
        PosicionJugadorDto dto = PosicionJugadorDto.movimiento("u1", new Posicion(2, 1),
                List.of(new Posicion(1, 1), new Posicion(2, 1)));

        JsonNode json = new ObjectMapper().valueToTree(dto);

        assertEquals("u1", json.get("userId").asText());
        assertEquals(2, json.get("x").asInt());
        assertTrue(json.get("conectado").asBoolean());
        assertEquals(2, json.get("ruta").size());
        assertEquals(1, json.get("ruta").get(0).get("x").asInt());
        assertEquals(1, json.get("ruta").get(0).get("y").asInt());
    }

    @Test
    void sinMovimientoLaRutaVaVaciaNuncaNull() {
        assertEquals(List.of(), PosicionJugadorDto.conectado("u1", new Posicion(0, 0)).ruta());
        assertEquals(List.of(), new PosicionJugadorDto("u1", 0, 0, false, null).ruta());
    }

    @Test
    void laRutaNoSePuedeModificarDesdeAfuera() {
        List<Posicion> ruta = new ArrayList<>(List.of(new Posicion(1, 1)));
        PosicionJugadorDto dto = PosicionJugadorDto.movimiento("u1", new Posicion(1, 1), ruta);

        ruta.add(new Posicion(9, 9));

        assertEquals(1, dto.ruta().size());
    }
}
