package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.AcertijoNoAbiertoException;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.AcertijoYaResueltoException;
import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.ElementoInteractivo;
import co.eci.c15.gameplay.domain.ElementoInteractivoRepository;
import co.eci.c15.gameplay.domain.ElementoNoValidoException;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.domain.TipoComponente;
import co.eci.c15.gameplay.infrastructure.persistence.AcertijosEnPartidaEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.JugadorEnMapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ClickAcertijoUseCaseTest {

    private static final String MATCH = "m1";
    private static final Posicion JUNTO = new Posicion(2, 3);

    private JugadorEnMapaRepositoryEnMemoria jugadores;
    private AcertijosEnPartidaEnMemoria estados;
    private AbrirAcertijoUseCase abrir;
    private ClickAcertijoUseCase clicks;

    @BeforeEach
    void setUp() {
        MapaRepositoryEnMemoria mapas = new MapaRepositoryEnMemoria();
        mapas.saveIfAbsent(new MapaIsometrico(MATCH, 6, 6, new Posicion(0, 0), List.of(
                new ComponenteMapa("acertijo-1", TipoComponente.ACERTIJO, new Posicion(2, 2)))));
        ObtenerMapaUseCase obtenerMapa = new ObtenerMapaUseCase(mapas, new GeneradorMapa());
        jugadores = new JugadorEnMapaRepositoryEnMemoria();
        estados = new AcertijosEnPartidaEnMemoria();
        AcertijoRepository catalogo = new CatalogoFijo();
        ElementoInteractivoRepository elementos = new ElementosFijos();
        abrir = new AbrirAcertijoUseCase(jugadores, obtenerMapa, catalogo, elementos, estados, CandadoEquipo.ninguno());
        clicks = new ClickAcertijoUseCase(jugadores, catalogo, elementos, estados, CandadoEquipo.ninguno());
    }

    private JugadorEnMapa jugador(String equipo, String userId) {
        return jugadores.saveIfAbsent(new JugadorEnMapa(MATCH, equipo, userId, JUNTO));
    }

    @Test
    void clickConValorSeRegistraEnOrden() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");

        clicks.click(MATCH, "acertijo-1", "ana", "lista", "rojo");
        EntradaAcertijoDto dto = clicks.click(MATCH, "acertijo-1", "ana", "lista", " naranja ");

        assertEquals(List.of("rojo", "naranja"), dto.entrada());
        assertFalse(dto.resuelto());
        assertEquals("acertijo-1", dto.componenteMapaId());
    }

    @Test
    void clickSinValorCuentaElIdDelElemento() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");

        assertEquals(List.of("lista"), clicks.click(MATCH, "acertijo-1", "ana", "lista", null).entrada());
        assertEquals(List.of("lista", "lista"), clicks.click(MATCH, "acertijo-1", "ana", "lista", "  ").entrada());
    }

    @Test
    void sinAbrirElAcertijoNoSePuedeInteractuar() {
        jugador("e1", "ana");
        assertThrows(AcertijoNoAbiertoException.class, () -> clicks.click(MATCH, "acertijo-1", "ana", "lista", "rojo"));
    }

    @Test
    void elCompaneroNoPuedeInteractuarMientrasOtroLoTieneAbierto() {
        jugador("e1", "ana");
        jugador("e1", "beto");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");

        assertThrows(AcertijoBloqueadoException.class, () -> abrir.ejecutar(MATCH, "acertijo-1", "beto"));
        AcertijoBloqueadoException enUso = assertThrows(AcertijoBloqueadoException.class,
                () -> clicks.click(MATCH, "acertijo-1", "beto", "lista", "rojo"));
        assertTrue(enUso.getMessage().contains("otro jugador"));
        assertTrue(estados.obtener(MATCH, "e1", "acertijo-1").getEntrada().isEmpty());
    }

    @Test
    void siElBloqueoPasoAOtroElAnteriorYaNoPuedeHacerClick() {
        JugadorEnMapa ana = jugador("e1", "ana");
        jugador("e1", "beto");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        jugadores.remove(MATCH, "ana"); // se desconecta
        abrir.ejecutar(MATCH, "acertijo-1", "beto");

        assertTrue(ana.getAcertijoAbierto().isPresent()); // su objeto ya no esta en la partida
        assertThrows(JugadorNoRegistradoException.class, () -> clicks.click(MATCH, "acertijo-1", "ana", "lista", "rojo"));
        assertDoesNotThrow(() -> clicks.click(MATCH, "acertijo-1", "beto", "lista", "azul"));
    }

    @Test
    void cadaEquipoTieneSuPropiaEntrada() {
        jugador("e1", "ana");
        jugador("e2", "carla");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "carla");

        clicks.click(MATCH, "acertijo-1", "ana", "lista", "rojo");

        assertEquals(List.of("rojo"), clicks.consultar(MATCH, "acertijo-1", "ana").entrada());
        assertTrue(clicks.consultar(MATCH, "acertijo-1", "carla").entrada().isEmpty());
    }

    @Test
    void cerrarElAcertijoDescartaLaEntradaDelEquipo() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        clicks.click(MATCH, "acertijo-1", "ana", "lista", "rojo");

        abrir.cerrar(MATCH, "acertijo-1", "ana");

        assertTrue(clicks.consultar(MATCH, "acertijo-1", "ana").entrada().isEmpty());
        assertThrows(AcertijoNoAbiertoException.class, () -> clicks.click(MATCH, "acertijo-1", "ana", "lista", "rojo"));
    }

    @Test
    void reiniciarBorraLoIngresadoYSeguirAbierto() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        clicks.click(MATCH, "acertijo-1", "ana", "lista", "rojo");

        EntradaAcertijoDto dto = clicks.reiniciar(MATCH, "acertijo-1", "ana");

        assertTrue(dto.entrada().isEmpty());
        assertDoesNotThrow(() -> clicks.click(MATCH, "acertijo-1", "ana", "lista", "azul"));
    }

    @Test
    void acertijoResueltoNoAceptaClicks() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        estados.obtener(MATCH, "e1", "acertijo-1").marcarResuelto();

        assertThrows(AcertijoYaResueltoException.class, () -> clicks.click(MATCH, "acertijo-1", "ana", "lista", "rojo"));
    }

    @Test
    void validaJugadorAcertijoYElemento() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");

        assertThrows(JugadorNoRegistradoException.class, () -> clicks.click(MATCH, "acertijo-1", "intruso", "lista", "x"));
        assertThrows(ComponenteSinAcertijoException.class, () -> clicks.click(MATCH, "acertijo-99", "ana", "lista", "x"));
        assertThrows(ElementoNoValidoException.class, () -> clicks.click(MATCH, "acertijo-1", "ana", "otro", "x"));
        assertThrows(ElementoNoValidoException.class, () -> clicks.click(MATCH, "acertijo-1", "ana", null, "x"));
        String muyLargo = "x".repeat(ClickAcertijoUseCase.MAX_LONGITUD_VALOR + 1);
        assertThrows(IllegalArgumentException.class, () -> clicks.click(MATCH, "acertijo-1", "ana", "lista", muyLargo));
        AcertijoEnPartida estado = estados.obtener(MATCH, "e1", "acertijo-1");
        assertTrue(estado.getEntrada().isEmpty());
    }

    private static class CatalogoFijo implements AcertijoRepository {
        private final Acertijo acertijo = new Acertijo("cat-1", "acertijo-1", "Ordena los colores");

        @Override
        public Optional<Acertijo> findByComponenteMapaId(String componenteMapaId) {
            return acertijo.getComponenteMapaId().equals(componenteMapaId) ? Optional.of(acertijo) : Optional.empty();
        }

        @Override
        public Acertijo save(Acertijo a) { return a; }
    }

    private static class ElementosFijos implements ElementoInteractivoRepository {
        private final List<ElementoInteractivo> elementos =
                List.of(new ElementoInteractivo("lista", "cat-1", "lista-ordenable", "Colores"));

        @Override
        public List<ElementoInteractivo> findByAcertijoId(String acertijoId) {
            return elementos.stream().filter(e -> e.getAcertijoId().equals(acertijoId)).toList();
        }

        @Override
        public ElementoInteractivo save(ElementoInteractivo e) { return e; }
    }
}
