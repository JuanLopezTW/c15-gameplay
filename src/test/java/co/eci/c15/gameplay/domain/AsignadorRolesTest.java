package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class AsignadorRolesTest {

    private static List<String> miembros(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(i -> "u" + i).toList();
    }

    private static long contar(Map<String, Rol> roles, Rol rol) {
        return roles.values().stream().filter(r -> r == rol).count();
    }

    @Test
    void equipoDeTresTieneDosHeroesYUnVerdugo() {
        Map<String, Rol> roles = new AsignadorRoles(new Random(1)).asignar(miembros(3));
        assertEquals(2, contar(roles, Rol.HEROE));
        assertEquals(1, contar(roles, Rol.VERDUGO));
    }

    @Test
    void todoMiembroRecibeExactamenteUnRol() {
        Map<String, Rol> roles = new AsignadorRoles(new Random(1)).asignar(miembros(5));
        assertEquals(new HashSet<>(miembros(5)), roles.keySet());
    }

    @Test
    void todoEquipoDeDosOMasTieneAlMenosUnHeroeYUnVerdugo() {
        for (int n = 2; n <= 12; n++) {
            Map<String, Rol> roles = new AsignadorRoles(new Random(n)).asignar(miembros(n));
            assertTrue(contar(roles, Rol.VERDUGO) >= 1, "verdugo en equipo de " + n);
            assertTrue(contar(roles, Rol.HEROE) >= 1, "heroe en equipo de " + n);
            assertEquals(n, roles.size());
            assertEquals(AsignadorRoles.cantidadVerdugos(n), contar(roles, Rol.VERDUGO));
        }
    }

    @Test
    void equipoDeUnoSoloTieneHeroeYVacioNoTieneNada() {
        assertEquals(Map.of("u1", Rol.HEROE), new AsignadorRoles(new Random(1)).asignar(miembros(1)));
        assertTrue(new AsignadorRoles(new Random(1)).asignar(List.of()).isEmpty());
    }

    @Test
    void elVerdugoEsAleatorioEntreLosMiembros() {
        Set<String> verdugos = new HashSet<>();
        for (int semilla = 0; semilla < 100; semilla++) {
            new AsignadorRoles(new Random(semilla)).asignar(miembros(3)).forEach((u, r) -> {
                if (r == Rol.VERDUGO) verdugos.add(u);
            });
        }
        assertEquals(Set.of("u1", "u2", "u3"), verdugos);
    }

    @Test
    void mismaSemillaDaMismoResultado() {
        assertEquals(new AsignadorRoles(new Random(7)).asignar(miembros(6)),
                new AsignadorRoles(new Random(7)).asignar(miembros(6)));
    }

    @Test
    void noModificaLaListaRecibida() {
        List<String> original = miembros(4);
        new AsignadorRoles(new Random(3)).asignar(original);
        assertEquals(miembros(4), original);
    }
}
