package co.eci.c15.gameplay.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Reparte al azar Heroes y Verdugos dentro de un equipo: un verdugo por cada tres
 * miembros (minimo uno) y el resto heroes. Un equipo de un solo jugador solo tiene heroe.
 * Con 3 jugadores quedan 2 heroes y 1 verdugo.
 */
public class AsignadorRoles {

    private final RandomGenerator azar;

    public AsignadorRoles(RandomGenerator azar) {
        this.azar = azar;
    }

    public static int cantidadVerdugos(int miembros) {
        return miembros < 2 ? 0 : Math.max(1, miembros / 3);
    }

    public Map<String, Rol> asignar(List<String> miembros) {
        List<String> orden = new ArrayList<>(miembros);
        Collections.shuffle(orden, azar);
        int verdugos = cantidadVerdugos(orden.size());
        Map<String, Rol> roles = new HashMap<>();
        for (int i = 0; i < orden.size(); i++) {
            roles.put(orden.get(i), i < verdugos ? Rol.VERDUGO : Rol.HEROE);
        }
        return roles;
    }
}
