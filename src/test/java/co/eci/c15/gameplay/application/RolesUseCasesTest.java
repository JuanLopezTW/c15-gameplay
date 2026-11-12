package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.AsignadorRoles;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.RolNoAsignadoException;
import co.eci.c15.gameplay.infrastructure.persistence.RolesRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RolesUseCasesTest {

    private static final Map<String, List<String>> EQUIPOS =
            Map.of("e1", List.of("a1", "a2", "a3"), "e2", List.of("b1", "b2", "b3", "b4"));

    private RolesRepositoryEnMemoria repo;
    private List<AsignacionRolesDto> avisos;
    private AsignarRolesUseCase asignar;
    private ConsultarRolesUseCase consultar;

    @BeforeEach
    void setUp() {
        repo = new RolesRepositoryEnMemoria();
        avisos = Collections.synchronizedList(new ArrayList<>());
        asignar = new AsignarRolesUseCase(new AsignadorRoles(new Random(42)), repo, avisos::add);
        consultar = new ConsultarRolesUseCase(repo);
    }

    @Test
    void cadaEquipoQuedaConRolesCompletos() {
        List<AsignacionRolesDto> resultado = asignar.ejecutar("p1", EQUIPOS);

        assertEquals(2, resultado.size());
        for (AsignacionRolesDto dto : resultado) {
            assertEquals(EQUIPOS.get(dto.equipoId()).size(), dto.roles().size());
            assertTrue(dto.roles().containsValue(Rol.HEROE));
            assertTrue(dto.roles().containsValue(Rol.VERDUGO));
        }
    }

    @Test
    void avisaUnaVezPorEquipoYSoloConSusMiembros() {
        asignar.ejecutar("p1", EQUIPOS);

        assertEquals(2, avisos.size());
        for (AsignacionRolesDto aviso : avisos) {
            assertEquals("p1", aviso.matchId());
            assertEquals(Set.copyOf(EQUIPOS.get(aviso.equipoId())), aviso.roles().keySet());
        }
    }

    @Test
    void asignarDeNuevoRespetaLosRolesYNoReavisa() {
        List<AsignacionRolesDto> primera = asignar.ejecutar("p1", EQUIPOS);
        avisos.clear();

        List<AsignacionRolesDto> segunda = asignar.ejecutar("p1", EQUIPOS);

        assertEquals(primera, segunda);
        assertTrue(avisos.isEmpty());
    }

    @Test
    void cadaPartidaTieneSusPropiosRoles() {
        asignar.ejecutar("p1", EQUIPOS);
        asignar.ejecutar("p2", EQUIPOS);

        assertEquals(4, avisos.size());
        assertNotNull(consultar.rolDe("p2", "a1"));
    }

    @Test
    void consultaElRolDeUnJugadorYLosRolesDeSuEquipo() {
        AsignacionRolesDto e1 = asignar.ejecutar("p1", EQUIPOS).stream()
                .filter(d -> d.equipoId().equals("e1")).findFirst().orElseThrow();

        assertEquals(e1.roles().get("a2"), consultar.rolDe("p1", "a2").rol());
        assertEquals(e1, consultar.rolesDelEquipo("p1", "e1"));
    }

    @Test
    void consultaSinAsignacionFalla() {
        assertThrows(RolNoAsignadoException.class, () -> consultar.rolDe("p1", "a1"));
        assertThrows(RolNoAsignadoException.class, () -> consultar.rolesDelEquipo("p1", "e1"));
        asignar.ejecutar("p1", EQUIPOS);
        assertThrows(RolNoAsignadoException.class, () -> consultar.rolDe("p1", "desconocido"));
    }

    @Test
    void rechazaPartidaOEquiposInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> asignar.ejecutar(" ", EQUIPOS));
        assertThrows(IllegalArgumentException.class, () -> asignar.ejecutar(null, EQUIPOS));
        assertThrows(IllegalArgumentException.class, () -> asignar.ejecutar("p1", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> asignar.ejecutar("p1", null));
    }

    @Test
    void llamadasConcurrentesDejanUnaSolaAsignacionPorEquipo() throws Exception {
        int hilos = 16;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<List<AsignacionRolesDto>>> futuros = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            futuros.add(pool.submit(() -> {
                salida.await();
                return new AsignarRolesUseCase(new AsignadorRoles(new Random()), repo, avisos::add).ejecutar("p1", EQUIPOS);
            }));
        }
        salida.countDown();
        List<AsignacionRolesDto> referencia = futuros.get(0).get(5, TimeUnit.SECONDS);
        for (Future<List<AsignacionRolesDto>> f : futuros) assertEquals(referencia, f.get(5, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(2, avisos.size());
    }
}
