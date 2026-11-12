package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.AcertijoNoAbiertoException;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.AcertijoYaResueltoException;
import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.ElementoInteractivo;
import co.eci.c15.gameplay.domain.ElementoInteractivoRepository;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.domain.SolucionAcertijoRepository;
import co.eci.c15.gameplay.domain.SolucionNoDisponibleException;
import co.eci.c15.gameplay.domain.TipoComponente;
import co.eci.c15.gameplay.infrastructure.persistence.AcertijosEnPartidaEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.JugadorEnMapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ValidarSolucionUseCaseTest {

    private static final String MATCH = "m1";
    private static final Posicion JUNTO = new Posicion(2, 3);

    private JugadorEnMapaRepositoryEnMemoria jugadores;
    private AcertijosEnPartidaEnMemoria estados;
    private List<Object> eventos;
    private AbrirAcertijoUseCase abrir;
    private ClickAcertijoUseCase clicks;
    private ValidarSolucionUseCase validar;

    @BeforeEach
    void setUp() {
        MapaRepositoryEnMemoria mapas = new MapaRepositoryEnMemoria();
        mapas.saveIfAbsent(new MapaIsometrico(MATCH, 6, 6, new Posicion(0, 0), List.of(
                new ComponenteMapa("acertijo-1", TipoComponente.ACERTIJO, new Posicion(2, 2)),
                new ComponenteMapa("acertijo-2", TipoComponente.ACERTIJO, new Posicion(4, 4)))));
        ObtenerMapaUseCase obtenerMapa = new ObtenerMapaUseCase(mapas, new GeneradorMapa());
        jugadores = new JugadorEnMapaRepositoryEnMemoria();
        estados = new AcertijosEnPartidaEnMemoria();
        eventos = new ArrayList<>();
        AcertijoRepository catalogo = new CatalogoFijo();
        ElementoInteractivoRepository elementos = new ElementosFijos();
        abrir = new AbrirAcertijoUseCase(jugadores, obtenerMapa, catalogo, elementos, estados, CandadoEquipo.ninguno());
        clicks = new ClickAcertijoUseCase(jugadores, catalogo, elementos, estados, CandadoEquipo.ninguno());
        validar = new ValidarSolucionUseCase(jugadores, catalogo, new SolucionesFijas(), estados, eventos::add, CandadoEquipo.ninguno());
    }

    private JugadorEnMapa jugador(String equipo, String userId) {
        return jugadores.saveIfAbsent(new JugadorEnMapa(MATCH, equipo, userId, JUNTO));
    }

    @Test
    void solucionCorrectaPorClicksResuelveYPublicaElEvento() {
        JugadorEnMapa ana = jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        clicks.click(MATCH, "acertijo-1", "ana", "e", "rojo");
        clicks.click(MATCH, "acertijo-1", "ana", "e", "azul");

        ResultadoSolucionDto r = validar.ejecutar(MATCH, "acertijo-1", "ana", null);

        assertTrue(r.correcto());
        assertTrue(r.resuelto());
        assertTrue(estados.obtener(MATCH, "e1", "acertijo-1").isResuelto());
        assertEquals(List.of(new AcertijoResueltoEvent(MATCH, "e1", "acertijo-1", "ana")), eventos);
        assertTrue(ana.getAcertijoAbierto().isEmpty(), "puede volver a moverse");
    }

    @Test
    void solucionCorrectaEnviadaDirectamente() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");

        assertTrue(validar.ejecutar(MATCH, "acertijo-1", "ana", List.of(" ROJO ", "Azul")).correcto());
    }

    @Test
    void solucionIncorrectaNoResuelveYPermiteReintentar() {
        JugadorEnMapa ana = jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        clicks.click(MATCH, "acertijo-1", "ana", "e", "azul");
        clicks.click(MATCH, "acertijo-1", "ana", "e", "rojo");

        ResultadoSolucionDto r = validar.ejecutar(MATCH, "acertijo-1", "ana", null);

        assertFalse(r.correcto());
        assertFalse(r.resuelto());
        assertFalse(estados.obtener(MATCH, "e1", "acertijo-1").isResuelto());
        assertTrue(eventos.isEmpty());
        assertTrue(estados.obtener(MATCH, "e1", "acertijo-1").getEntrada().isEmpty());
        assertTrue(ana.getAcertijoAbierto().isPresent(), "sigue con el acertijo abierto");

        assertTrue(validar.ejecutar(MATCH, "acertijo-1", "ana", List.of("rojo", "azul")).correcto());
    }

    @Test
    void sinNadaIngresadoEsIncorrecto() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        assertFalse(validar.ejecutar(MATCH, "acertijo-1", "ana", null).correcto());
        assertFalse(validar.ejecutar(MATCH, "acertijo-1", "ana", List.of()).correcto());
    }

    @Test
    void elCompaneroDeEquipoVeElAcertijoResueltoPeroElOtroEquipoNo() {
        jugador("e1", "ana");
        jugador("e1", "beto");
        jugador("e2", "carla");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        validar.ejecutar(MATCH, "acertijo-1", "ana", List.of("rojo", "azul"));

        assertTrue(abrir.ejecutar(MATCH, "acertijo-1", "beto").resuelto());
        assertFalse(abrir.ejecutar(MATCH, "acertijo-1", "carla").resuelto());
    }

    @Test
    void unAcertijoResueltoNoSePuedeVolverAResolver() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        validar.ejecutar(MATCH, "acertijo-1", "ana", List.of("rojo", "azul"));

        List<String> respuestaCorrecta = List.of("rojo", "azul");
        assertThrows(AcertijoYaResueltoException.class, () -> validar.ejecutar(MATCH, "acertijo-1", "ana", respuestaCorrecta));
        assertEquals(1, eventos.size());
    }

    @Test
    void sinAbrirElAcertijoNoSePuedeEnviarSolucion() {
        jugador("e1", "ana");
        List<String> respuestaCorrecta = List.of("rojo", "azul");
        assertThrows(AcertijoNoAbiertoException.class, () -> validar.ejecutar(MATCH, "acertijo-1", "ana", respuestaCorrecta));
        assertTrue(eventos.isEmpty());
    }

    @Test
    void unCompaneroNoPuedeResolverloMientrasOtroLoTieneAbierto() {
        jugador("e1", "ana");
        jugador("e1", "beto");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");

        List<String> respuestaCorrecta = List.of("rojo", "azul");
        assertThrows(AcertijoBloqueadoException.class, () -> validar.ejecutar(MATCH, "acertijo-1", "beto", respuestaCorrecta));
        assertFalse(estados.obtener(MATCH, "e1", "acertijo-1").isResuelto());
    }

    @Test
    void validaJugadorAcertijoYSolucionConfigurada() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");

        assertThrows(JugadorNoRegistradoException.class, () -> validar.ejecutar(MATCH, "acertijo-1", "intruso", null));
        assertThrows(ComponenteSinAcertijoException.class, () -> validar.ejecutar(MATCH, "acertijo-99", "ana", null));
        assertThrows(SolucionNoDisponibleException.class, () -> validar.ejecutar(MATCH, "acertijo-2", "ana", null));
    }

    @Test
    void rechazaRespuestasDesmedidas() {
        jugador("e1", "ana");
        abrir.ejecutar(MATCH, "acertijo-1", "ana");
        List<String> muchos = java.util.Collections.nCopies(ValidarSolucionUseCase.MAX_PASOS + 1, "x");
        List<String> largo = List.of("x".repeat(ValidarSolucionUseCase.MAX_LONGITUD_PASO + 1));
        List<String> conNulo = java.util.Arrays.asList("a", null);

        assertThrows(IllegalArgumentException.class, () -> validar.ejecutar(MATCH, "acertijo-1", "ana", muchos));
        assertThrows(IllegalArgumentException.class, () -> validar.ejecutar(MATCH, "acertijo-1", "ana", largo));
        assertThrows(IllegalArgumentException.class, () -> validar.ejecutar(MATCH, "acertijo-1", "ana", conNulo));
    }

    private static class SolucionesFijas implements SolucionAcertijoRepository {
        @Override
        public Optional<List<String>> findByComponenteMapaId(String componenteMapaId) {
            return "acertijo-1".equals(componenteMapaId) ? Optional.of(List.of("rojo", "azul")) : Optional.empty();
        }
    }

    private static class CatalogoFijo implements AcertijoRepository {
        @Override
        public Optional<Acertijo> findByComponenteMapaId(String componenteMapaId) {
            return componenteMapaId.startsWith("acertijo-") && !componenteMapaId.equals("acertijo-99")
                    ? Optional.of(new Acertijo("cat-" + componenteMapaId, componenteMapaId, "Enunciado"))
                    : Optional.empty();
        }

        @Override
        public Acertijo save(Acertijo a) { return a; }
    }

    private static class ElementosFijos implements ElementoInteractivoRepository {
        @Override
        public List<ElementoInteractivo> findByAcertijoId(String acertijoId) {
            return List.of(new ElementoInteractivo("e", acertijoId, "lista-ordenable", "Colores"));
        }

        @Override
        public ElementoInteractivo save(ElementoInteractivo e) { return e; }
    }
}
