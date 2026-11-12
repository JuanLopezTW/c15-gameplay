package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.InventarioEquipo;
import co.eci.c15.gameplay.domain.InventarioRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class InventarioRepositoryEnMemoria implements InventarioRepository {

    private record Clave(String matchId, String equipoId) {}

    private final Map<Clave, InventarioEquipo> inventarios = new ConcurrentHashMap<>();

    @Override
    public InventarioEquipo obtener(String matchId, String equipoId) {
        return inventarios.computeIfAbsent(new Clave(matchId, equipoId), c -> new InventarioEquipo(matchId, equipoId));
    }

    @Override
    public void guardar(InventarioEquipo inventario) {
        inventarios.put(new Clave(inventario.getMatchId(), inventario.getEquipoId()), inventario);
    }
}
