package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.MapaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class MapaRepositoryEnMemoria implements MapaRepository {

    private final Map<String, MapaIsometrico> mapas = new ConcurrentHashMap<>();

    @Override
    public Optional<MapaIsometrico> findByMatchId(String matchId) {
        return Optional.ofNullable(mapas.get(matchId));
    }

    @Override
    public MapaIsometrico saveIfAbsent(MapaIsometrico mapa) {
        MapaIsometrico existente = mapas.putIfAbsent(mapa.getMatchId(), mapa);
        return existente != null ? existente : mapa;
    }
}
