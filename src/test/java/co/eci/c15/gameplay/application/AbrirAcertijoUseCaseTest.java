package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.*;
import co.eci.c15.gameplay.infrastructure.persistence.AcertijosEnPartidaEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.JugadorEnMapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AbrirAcertijoUseCaseTest {

    private static final String MATCH = "match-1";
    private static final Posicion JUNTO_A_ACERTIJO_1 = new Posicion(2, 3);

    private JugadorEnMapaRepositoryEnMemoria jugadores;
    private AcertijosEnPartidaEnMemoria estados;
    private RelojFijo reloj;
    private AbrirAcertijoUseCase useCase;

    @BeforeEach
    void setUp() {
        MapaRepositoryEnMemoria mapas = new MapaRepositoryEnMemoria();
        mapas.saveIfAbsent(new MapaIsometrico(MATCH, 6, 6, new Posicion(0, 0), List.of(
                new ComponenteMapa("acertijo-1", TipoComponente.ACERTIJO, new Posicion(2, 2)),
                new ComponenteMapa("llave-1", TipoComponente.LLAVE, new Posicion(4, 4)))));
        ObtenerMapaUseCase obtenerMapa = new ObtenerMapaUseCase(mapas, new GeneradorMapa());

        jugadores = new JugadorEnMapaRepositoryEnMemoria();
        estados = new AcertijosEnPartidaEnMemoria();
        reloj = new RelojFijo(Instant.parse("2026-10-08T10:00:00Z"));
        useCase = new AbrirAcertijoUseCase(jugadores, obtenerMapa, new CatalogoFijo(), new ElementosFijos(), estados, CandadoEquipo.ninguno(), reloj);
    }

    private JugadorEnMapa jugador(String equipo, String userId, Posicion posicion) {
        return jugadores.saveIfAbsent(new JugadorEnMapa(MATCH, equipo, userId, posicion));
    }

    @Test
    void abreElAcertijoConSuContenido() {
        jugador("equipo-1", "ana", JUNTO_A_ACERTIJO_1);

        VisualizarAcertijoDto dto = useCase.ejecutar(MATCH, "acertijo-1", "ana");

        assertEquals("acertijo-1", dto.componenteMapaId());
        assertEquals("¿Cuánto es 2+2?", dto.enunciado());
        assertEquals("PENDIENTE", dto.estado());
        assertTrue(dto.puedeEnviarSolucion());
        assertEquals(1, dto.elementos().size());
    }

    @Test
    void companeroNoPuedeAbrirloMientrasEsteAbierto() {
        jugador("equipo-1", "ana", JUNTO_A_ACERTIJO_1);
        jugador("equipo-1", "beto", JUNTO_A_ACERTIJO_1);
        useCase.ejecutar(MATCH, "acertijo-1", "ana");

        assertThrows(AcertijoBloqueadoException.class, () -> useCase.ejecutar(MATCH, "acertijo-1", "beto"));
    }

    @Test
    void elOtroEquipoTieneSuPropioAcertijo() {
        jugador("equipo-1", "ana", JUNTO_A_ACERTIJO_1);
        jugador("equipo-2", "carla", JUNTO_A_ACERTIJO_1);
        useCase.ejecutar(MATCH, "acertijo-1", "ana");

        assertDoesNotThrow(() -> useCase.ejecutar(MATCH, "acertijo-1", "carla"));
    }

    @Test
    void lejosDelAcertijoNoSePuedeAbrir() {
        jugador("equipo-1", "ana", new Posicion(0, 0));
        assertThrows(AcertijoLejosException.class, () -> useCase.ejecutar(MATCH, "acertijo-1", "ana"));
    }

    @Test
    void componenteQueNoEsAcertijoDa404() {
        jugador("equipo-1", "ana", new Posicion(4, 3));
        assertThrows(ComponenteSinAcertijoException.class, () -> useCase.ejecutar(MATCH, "llave-1", "ana"));
        assertThrows(ComponenteSinAcertijoException.class, () -> useCase.ejecutar(MATCH, "acertijo-99", "ana"));
    }

    @Test
    void jugadorFueraDeLaPartidaNoPuedeAbrir() {
        assertThrows(JugadorNoRegistradoException.class, () -> useCase.ejecutar(MATCH, "acertijo-1", "intruso"));
    }

    @Test
    void alCerrarloElCompaneroPuedeAbrirloYElPrimeroMoverse() {
        JugadorEnMapa ana = jugador("equipo-1", "ana", JUNTO_A_ACERTIJO_1);
        jugador("equipo-1", "beto", JUNTO_A_ACERTIJO_1);
        useCase.ejecutar(MATCH, "acertijo-1", "ana");

        useCase.cerrar(MATCH, "acertijo-1", "ana");

        assertTrue(ana.getAcertijoAbierto().isEmpty());
        assertDoesNotThrow(() -> useCase.ejecutar(MATCH, "acertijo-1", "beto"));
    }

    @Test
    void siElPoseedorSeDesconectaElCompaneroLoToma() {
        jugador("equipo-1", "ana", JUNTO_A_ACERTIJO_1);
        jugador("equipo-1", "beto", JUNTO_A_ACERTIJO_1);
        useCase.ejecutar(MATCH, "acertijo-1", "ana");

        jugadores.remove(MATCH, "ana"); // lo que hace DesconectarJugadorUseCase

        assertDoesNotThrow(() -> useCase.ejecutar(MATCH, "acertijo-1", "beto"));
    }

    @Test
    void trasElTtlElCompaneroLoTomaYElAnteriorPuedeMoverse() {
        JugadorEnMapa ana = jugador("equipo-1", "ana", JUNTO_A_ACERTIJO_1);
        jugador("equipo-1", "beto", JUNTO_A_ACERTIJO_1);
        useCase.ejecutar(MATCH, "acertijo-1", "ana");

        reloj.avanzar(AbrirAcertijoUseCase.TTL_BLOQUEO);
        useCase.ejecutar(MATCH, "acertijo-1", "beto");

        assertEquals(Optional.of("beto"), estados.obtener(MATCH, "equipo-1", "acertijo-1").getPoseedor());
        assertTrue(ana.getAcertijoAbierto().isEmpty());
    }

    @Test
    void acertijoResueltoSeVeSinBloquear() {
        JugadorEnMapa ana = jugador("equipo-1", "ana", JUNTO_A_ACERTIJO_1);
        estados.obtener(MATCH, "equipo-1", "acertijo-1").marcarResuelto();

        VisualizarAcertijoDto dto = useCase.ejecutar(MATCH, "acertijo-1", "ana");

        assertTrue(dto.resuelto());
        assertFalse(dto.puedeEnviarSolucion());
        assertTrue(ana.getAcertijoAbierto().isEmpty());
    }

    private static class CatalogoFijo implements AcertijoRepository {
        private final Acertijo acertijo = new Acertijo("cat-1", "acertijo-1", "¿Cuánto es 2+2?");

        @Override
        public Optional<Acertijo> findByComponenteMapaId(String componenteMapaId) {
            return acertijo.getComponenteMapaId().equals(componenteMapaId) ? Optional.of(acertijo) : Optional.empty();
        }

        @Override
        public Acertijo save(Acertijo a) { return a; }
    }

    private static class ElementosFijos implements ElementoInteractivoRepository {
        private final List<ElementoInteractivo> elementos = new ArrayList<>(List.of(
                new ElementoInteractivo("e1", "cat-1", "campo-numero", "Respuesta")));

        @Override
        public List<ElementoInteractivo> findByAcertijoId(String acertijoId) {
            return elementos.stream().filter(e -> e.getAcertijoId().equals(acertijoId)).toList();
        }

        @Override
        public ElementoInteractivo save(ElementoInteractivo e) { return e; }
    }

    private static class RelojFijo extends Clock {
        private Instant ahora;

        RelojFijo(Instant ahora) { this.ahora = ahora; }

        void avanzar(java.time.Duration d) { ahora = ahora.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }
}
