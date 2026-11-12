package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.InventarioEquipo;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.ObjetoInventario;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.domain.ProgresoEquipo;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.TipoComponente;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Cada repositorio de gameplay contra un Redis real: lo que se guarda vuelve igual y cada clave caduca. */
class RepositoriosRedisTest extends RedisDePrueba {

    private static final Instant AHORA = Instant.parse("2026-11-03T15:00:00Z");
    private static final Posicion SPAWN = new Posicion(1, 1);

    // ---------- jugadores ----------

    @Test
    void jugadorElPrimeroQueSeRegistraGanaYSusCambiosSeGuardan() {
        JugadorEnMapaRedis jugadores = new JugadorEnMapaRedis(estado);
        JugadorEnMapa ana = new JugadorEnMapa("m1", "e1", "ana", SPAWN);

        assertSame(ana, jugadores.saveIfAbsent(ana));
        JugadorEnMapa repetido = jugadores.saveIfAbsent(new JugadorEnMapa("m1", "e2", "ana", new Posicion(0, 0)));
        assertEquals("e1", repetido.getEquipoId());

        JugadorEnMapa leido = jugadores.find("m1", "ana").orElseThrow();
        JugadorEnMapa movido = JugadorEnMapa.restaurar("m1", "e1", "ana", new Posicion(2, 1), "acertijo-1");
        jugadores.guardar(movido);

        JugadorEnMapa otraVez = jugadores.find("m1", "ana").orElseThrow();
        assertEquals(SPAWN, leido.getPosicion());
        assertEquals(new Posicion(2, 1), otraVez.getPosicion());
        assertEquals("acertijo-1", otraVez.getAcertijoAbierto().orElseThrow());
        assertEquals(1, jugadores.findByMatchId("m1").size());
        long vida = segundosDeVida(EstadoPartidaRedis.clave("m1", "jugadores"));
        assertTrue(vida > 1450 && vida <= 1500, "caduca en " + vida);
    }

    @Test
    void unJugadorQueSalioNoReapareceAlGuardarlo() {
        JugadorEnMapaRedis jugadores = new JugadorEnMapaRedis(estado);
        JugadorEnMapa ana = jugadores.saveIfAbsent(new JugadorEnMapa("m1", "e1", "ana", SPAWN));

        assertEquals("ana", jugadores.remove("m1", "ana").orElseThrow().getUserId());
        jugadores.guardar(ana);

        assertTrue(jugadores.find("m1", "ana").isEmpty());
        assertTrue(jugadores.remove("m1", "ana").isEmpty());
        assertTrue(jugadores.findByMatchId("otra").isEmpty());
    }

    // ---------- acertijos ----------

    @Test
    void acertijoSeCreaAlPedirloYConservaPoseedorBloqueoEntradaYEstado() {
        AcertijosEnPartidaRedis acertijos = new AcertijosEnPartidaRedis(estado);
        AcertijoEnPartida nuevo = acertijos.obtener("m1", "e1", "acertijo-1");
        assertFalse(nuevo.isResuelto());

        nuevo.bloquear("ana", AHORA, Duration.ofMinutes(5), u -> true);
        nuevo.registrarClick("ana", "rojo");
        acertijos.guardar(nuevo);

        AcertijoEnPartida leido = acertijos.obtener("m1", "e1", "acertijo-1");
        assertEquals("ana", leido.getPoseedor().orElseThrow());
        assertEquals(AHORA, leido.getBloqueadoEn().orElseThrow());
        assertEquals(List.of("rojo"), leido.getEntrada());
        // Otro equipo tiene su propio estado del mismo acertijo
        assertTrue(acertijos.obtener("m1", "e2", "acertijo-1").getPoseedor().isEmpty());

        leido.marcarResuelto();
        acertijos.guardar(leido);
        assertTrue(acertijos.obtener("m1", "e1", "acertijo-1").isResuelto());
    }

    // ---------- inventario ----------

    @Test
    void inventarioVacioNoSeGuardaYLoRecolectadoSi() {
        InventarioRedis inventarios = new InventarioRedis(estado);
        InventarioEquipo vacio = inventarios.obtener("m1", "e1");
        assertTrue(vacio.getObjetos().isEmpty());
        assertFalse(redis.hasKey(EstadoPartidaRedis.clave("m1", "inventarios")));

        vacio.agregar(new ObjetoInventario("llave-1", "Llave 1", "ana"));
        inventarios.guardar(vacio);

        assertEquals(List.of(new ObjetoInventario("llave-1", "Llave 1", "ana")), inventarios.obtener("m1", "e1").getObjetos());
        assertTrue(inventarios.obtener("m1", "e2").getObjetos().isEmpty());
    }

    // ---------- progreso ----------

    @Test
    void progresoSeGuardaDesdeElInicioYSaleOrdenadoPorEquipo() {
        ProgresoRedis progresos = new ProgresoRedis(estado);
        progresos.obtener("m1", "e2", 3);
        ProgresoEquipo e1 = progresos.obtener("m1", "e1", 3);

        e1.registrarResuelto("acertijo-1");
        progresos.guardar(e1);

        List<ProgresoEquipo> todos = progresos.findByMatchId("m1");
        assertEquals(List.of("e1", "e2"), todos.stream().map(ProgresoEquipo::getEquipoId).toList());
        assertEquals(List.of("acertijo-1"), todos.get(0).getResueltos());
        assertEquals(3, todos.get(0).getTotal());
        assertEquals(0, todos.get(1).cantidadResueltos());
        // Pedirlo otra vez no lo reinicia
        assertEquals(1, progresos.obtener("m1", "e1", 3).cantidadResueltos());
    }

    // ---------- roles ----------

    @Test
    void rolesLaPrimeraAsignacionGanaYSeBuscaElRolDeCadaJugador() {
        RolesRedis roles = new RolesRedis(estado);
        AsignacionRoles primera = new AsignacionRoles("m1", "e1", Map.of("ana", Rol.HEROE, "beto", Rol.VERDUGO));

        assertSame(primera, roles.saveIfAbsent(primera));
        AsignacionRoles otra = new AsignacionRoles("m1", "e1", Map.of("ana", Rol.VERDUGO, "beto", Rol.HEROE));
        AsignacionRoles guardada = roles.saveIfAbsent(otra);

        assertNotSame(otra, guardada);
        assertEquals(primera, guardada);
        assertEquals(Rol.VERDUGO, roles.findRol("m1", "beto").orElseThrow());
        assertTrue(roles.findRol("m1", "nadie").isEmpty());
        assertTrue(roles.findByEquipo("m1", "e9").isEmpty());
    }

    // ---------- mapa ----------

    @Test
    void mapaElPrimeroGanaYSeLeeIgualDesdeOtraInstancia() {
        MapaRedis instanciaA = new MapaRedis(estado);
        MapaRedis instanciaB = new MapaRedis(estado);
        MapaIsometrico generado = new GeneradorMapa().generar("m1");

        assertSame(generado, instanciaA.saveIfAbsent(generado));
        MapaIsometrico otro = new GeneradorMapa().generar("m1");
        MapaIsometrico deB = instanciaB.saveIfAbsent(otro);

        assertEquals(generado.getComponentes(), deB.getComponentes());
        assertEquals(generado.getSpawn(), deB.getSpawn());
        assertEquals(generado.getAncho(), deB.getAncho());
        assertTrue(instanciaB.tieneCopia("m1"));
        long vida = segundosDeVida(EstadoPartidaRedis.clave("m1", "mapa"));
        assertTrue(vida > 1450 && vida <= 1500, "caduca en " + vida);
    }

    @Test
    void laCopiaLocalDelMapaSeOlvidaYVenceSola() {
        MapaIsometrico mapa = new MapaIsometrico("m1", 3, 3, new Posicion(0, 0),
                List.of(new ComponenteMapa("acertijo-1", TipoComponente.ACERTIJO, new Posicion(2, 2))));
        MapaRedis instancia = new MapaRedis(estado);
        instancia.saveIfAbsent(mapa);

        instancia.olvidar("m1");
        assertFalse(instancia.tieneCopia("m1"));
        assertEquals(mapa.getComponentes(), instancia.findByMatchId("m1").orElseThrow().getComponentes());

        // Pasada la vida de la partida la copia no se usa: se consulta Redis (que ya lo borro)
        Instant[] ahora = {Instant.parse("2026-11-03T15:00:00Z")};
        Clock reloj = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zona) { return this; }
            @Override public Instant instant() { return ahora[0]; }
        };
        MapaRedis masTarde = new MapaRedis(estado, reloj);
        masTarde.findByMatchId("m1");
        assertTrue(masTarde.tieneCopia("m1"));
        redis.delete(EstadoPartidaRedis.clave("m1", "mapa"));
        assertTrue(masTarde.findByMatchId("m1").isPresent(), "mientras dure la partida usa su copia");
        ahora[0] = ahora[0].plus(estado.vida()).plusSeconds(1);
        assertTrue(masTarde.findByMatchId("m1").isEmpty(), "la copia vencida no debe usarse");
    }
}
