package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.DefinicionAcertijo;
import co.eci.c15.gameplay.domain.ElementoInteractivo;
import co.eci.c15.gameplay.domain.InventarioEquipo;
import co.eci.c15.gameplay.domain.ObjetoInventario;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.infrastructure.persistence.InventarioRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.RolesRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VistaAcertijoTest {

    private static final String MATCH = "m1";
    private static final String EQUIPO = "e1";

    private static final DefinicionAcertijo RELOJ = new DefinicionAcertijo("reloj-x", "reloj", "Pon la hora",
            List.of(new ElementoInteractivo("reloj-x-elemento-1", "reloj-x", "reloj", "Hora")), Map.of(),
            List.of(Map.of("pistas", List.of("I")), Map.of("pistas", List.of("II"))),
            null, Map.of(), List.of(List.of("9:45")));

    private static final DefinicionAcertijo CUADRO = new DefinicionAcertijo("uv-x", "uv", "Un cuadro",
            List.of(), Map.of("cuadro", "Retrato"), List.of(),
            "lampara-uv", Map.of("oculto", "LUNA"), List.of(List.of("luna")));

    private RolesRepositoryEnMemoria roles;
    private InventarioRepositoryEnMemoria inventarios;
    private VistaAcertijo vista;
    private final AcertijoEnPartida estado = new AcertijoEnPartida(MATCH, EQUIPO, "acertijo-1");

    @BeforeEach
    void setUp() {
        roles = new RolesRepositoryEnMemoria();
        inventarios = new InventarioRepositoryEnMemoria();
        vista = new VistaAcertijo(roles, inventarios);
    }

    @Test
    void elHeroeYElVerdugoVenPistasDistintas() {
        roles.saveIfAbsent(new AsignacionRoles(MATCH, EQUIPO, Map.of("ana", Rol.HEROE, "beto", Rol.VERDUGO)));

        assertEquals(List.of("I"), vista.para(MATCH, EQUIPO, "ana", "acertijo-1", RELOJ, estado).datos().get("pistas"));
        assertEquals(List.of("II"), vista.para(MATCH, EQUIPO, "beto", "acertijo-1", RELOJ, estado).datos().get("pistas"));
    }

    @Test
    void quienEstaSoloEnSuEquipoVeTodasLasPistas() {
        roles.saveIfAbsent(new AsignacionRoles(MATCH, EQUIPO, Map.of("ana", Rol.HEROE)));
        assertEquals(List.of("I", "II"), vista.para(MATCH, EQUIPO, "ana", "acertijo-1", RELOJ, estado).datos().get("pistas"));
    }

    @Test
    void sinRolesAsignadosTambienLasVeTodas() {
        assertEquals(List.of("I", "II"), vista.para(MATCH, EQUIPO, "ana", "acertijo-1", RELOJ, estado).datos().get("pistas"));
    }

    @Test
    void elCuadroSeRevelaCuandoElEquipoTieneLaLampara() {
        VisualizarAcertijoDto sinLampara = vista.para(MATCH, EQUIPO, "ana", "acertijo-2", CUADRO, estado);
        assertFalse(sinLampara.tieneObjeto());
        assertEquals("lampara-uv", sinLampara.objetoRequerido());
        assertFalse(sinLampara.datos().containsKey("oculto"));

        InventarioEquipo inventario = inventarios.obtener(MATCH, EQUIPO);
        inventario.agregar(new ObjetoInventario("lampara-uv", "Lámpara UV", "beto"));
        inventarios.guardar(inventario);

        VisualizarAcertijoDto conLampara = vista.para(MATCH, EQUIPO, "ana", "acertijo-2", CUADRO, estado);
        assertTrue(conLampara.tieneObjeto());
        assertEquals("LUNA", conLampara.datos().get("oculto"));
        assertEquals("uv", conLampara.tipo());
        assertEquals("acertijo-2", conLampara.componenteMapaId());
    }
}
