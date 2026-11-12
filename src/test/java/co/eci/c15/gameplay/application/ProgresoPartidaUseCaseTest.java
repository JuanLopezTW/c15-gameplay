package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.domain.TipoComponente;
import co.eci.c15.gameplay.infrastructure.persistence.MapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.ProgresoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ProgresoPartidaUseCaseTest {

    private static final String MATCH = "m1";

    private List<ProgresoPartidaDto> avisos;
    private ProgresoPartidaUseCase useCase;

    @BeforeEach
    void setUp() {
        MapaRepositoryEnMemoria mapas = new MapaRepositoryEnMemoria();
        mapas.saveIfAbsent(new MapaIsometrico(MATCH, 6, 6, new Posicion(0, 0), List.of(
                new ComponenteMapa("acertijo-1", TipoComponente.ACERTIJO, new Posicion(2, 2)),
                new ComponenteMapa("acertijo-2", TipoComponente.ACERTIJO, new Posicion(4, 4)),
                new ComponenteMapa("llave-1", TipoComponente.LLAVE, new Posicion(1, 4)))));
        avisos = Collections.synchronizedList(new ArrayList<>());
        useCase = new ProgresoPartidaUseCase(new ProgresoRepositoryEnMemoria(),
                new ObtenerMapaUseCase(mapas, new GeneradorMapa()), avisos::add, CandadoEquipo.ninguno());
    }

    private static AcertijoResueltoEvent resuelto(String equipo, String acertijo) {
        return new AcertijoResueltoEvent(MATCH, equipo, acertijo, "ana");
    }

    private static ProgresoPartidaDto.EquipoDto equipo(ProgresoPartidaDto dto, String equipoId) {
        return dto.equipos().stream().filter(e -> e.equipoId().equals(equipoId)).findFirst().orElseThrow();
    }

    @Test
    void alIniciarTodosLosEquiposAparecenEnCeroSobreElTotalDelMapa() {
        useCase.iniciar(MATCH, Set.of("e2", "e1"));

        ProgresoPartidaDto dto = useCase.consultar(MATCH);
        assertEquals(List.of("e1", "e2"), dto.equipos().stream().map(ProgresoPartidaDto.EquipoDto::equipoId).toList());
        assertTrue(dto.equipos().stream().allMatch(e -> e.resueltos() == 0 && e.total() == 2 && !e.completo()));
        assertEquals(1, avisos.size());
    }

    @Test
    void elTotalSoloCuentaLosAcertijosDelMapa() {
        useCase.iniciar(MATCH, Set.of("e1"));
        assertEquals(2, equipo(useCase.consultar(MATCH), "e1").total());
    }

    @Test
    void resolverUnAcertijoSumaAlEquipoYAvisaATodos() {
        useCase.iniciar(MATCH, Set.of("e1", "e2"));
        avisos.clear();

        useCase.acertijoResuelto(resuelto("e1", "acertijo-1"));

        assertEquals(1, avisos.size());
        ProgresoPartidaDto aviso = avisos.get(0);
        assertEquals(1, equipo(aviso, "e1").resueltos());
        assertEquals(List.of("acertijo-1"), equipo(aviso, "e1").acertijosResueltos());
        assertEquals(0, equipo(aviso, "e2").resueltos());
        assertEquals(aviso, useCase.consultar(MATCH));
    }

    @Test
    void completarTodosLosAcertijosMarcaAlEquipoComoCompleto() {
        useCase.iniciar(MATCH, Set.of("e1", "e2"));

        useCase.acertijoResuelto(resuelto("e1", "acertijo-1"));
        useCase.acertijoResuelto(resuelto("e1", "acertijo-2"));

        ProgresoPartidaDto dto = useCase.consultar(MATCH);
        assertTrue(equipo(dto, "e1").completo());
        assertFalse(equipo(dto, "e2").completo());
    }

    @Test
    void elMismoEventoDosVecesNoSumaNiVuelveAAvisar() {
        useCase.iniciar(MATCH, Set.of("e1"));
        useCase.acertijoResuelto(resuelto("e1", "acertijo-1"));
        avisos.clear();

        useCase.acertijoResuelto(resuelto("e1", "acertijo-1"));

        assertTrue(avisos.isEmpty());
        assertEquals(1, equipo(useCase.consultar(MATCH), "e1").resueltos());
    }

    @Test
    void unEquipoQueResuelveSinHaberIniciadoSeRegistraIgual() {
        useCase.acertijoResuelto(resuelto("e9", "acertijo-1"));

        assertEquals(1, equipo(useCase.consultar(MATCH), "e9").resueltos());
        assertEquals(1, avisos.size());
    }

    @Test
    void consultarUnaPartidaSinProgresoDevuelveVacio() {
        assertTrue(useCase.consultar("otra").equipos().isEmpty());
    }

    @Test
    void cadaPartidaTieneSuPropioProgreso() {
        useCase.iniciar(MATCH, Set.of("e1"));
        useCase.acertijoResuelto(resuelto("e1", "acertijo-1"));

        assertTrue(useCase.consultar("otra").equipos().isEmpty());
    }

    @Test
    void equiposResolviendoAlMismoTiempoNoPierdenNingunAcertijoYLosAvisosNuncaRetroceden() throws Exception {
        Set<String> equipos = Set.of("e1", "e2", "e3", "e4");
        useCase.iniciar(MATCH, equipos);
        avisos.clear();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch salida = new CountDownLatch(1);
        for (String e : equipos) {
            for (String a : List.of("acertijo-1", "acertijo-2")) {
                pool.submit(() -> {
                    salida.await();
                    useCase.acertijoResuelto(resuelto(e, a));
                    return null;
                });
            }
        }
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        ProgresoPartidaDto dto = useCase.consultar(MATCH);
        assertTrue(dto.equipos().stream().allMatch(e -> e.resueltos() == 2 && e.completo()));
        assertEquals(8, avisos.size());
        int anterior = 0;
        for (ProgresoPartidaDto aviso : avisos) {
            int suma = aviso.equipos().stream().mapToInt(ProgresoPartidaDto.EquipoDto::resueltos).sum();
            assertTrue(suma > anterior, "cada aviso trae mas progreso que el anterior");
            anterior = suma;
        }
    }
}
