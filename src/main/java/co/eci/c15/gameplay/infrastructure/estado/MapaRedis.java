package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.MapaRepository;
import co.eci.c15.gameplay.domain.Posicion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El mapa se guarda una vez en Redis (el primero que lo genera gana) y cada instancia se queda con una copia
 * local, porque se lee en cada movimiento y nunca cambia durante la partida. La copia se borra cuando la
 * partida termina (aviso por Redis a todas las instancias, ver {@link FinPartidaRedisListener}) y, por si el
 * aviso no llega, cuando pasa la vida de la partida.
 */
@Repository
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class MapaRedis implements MapaRepository {

    record Guardado(String matchId, int ancho, int alto, Posicion spawn, List<ComponenteMapa> componentes) {
        static Guardado de(MapaIsometrico m) {
            return new Guardado(m.getMatchId(), m.getAncho(), m.getAlto(), m.getSpawn(), m.getComponentes());
        }

        MapaIsometrico restaurar() {
            return new MapaIsometrico(matchId, ancho, alto, spawn, componentes);
        }
    }

    private record EnCopia(MapaIsometrico mapa, Instant hasta) {}

    private final EstadoPartidaRedis estado;
    private final Clock clock;
    private final Map<String, EnCopia> copias = new ConcurrentHashMap<>();

    @Autowired
    public MapaRedis(EstadoPartidaRedis estado) {
        this(estado, Clock.systemUTC());
    }

    MapaRedis(EstadoPartidaRedis estado, Clock clock) {
        this.estado = estado;
        this.clock = clock;
    }

    private static String clave(String matchId) {
        return EstadoPartidaRedis.clave(matchId, "mapa");
    }

    @Override
    public Optional<MapaIsometrico> findByMatchId(String matchId) {
        EnCopia copia = copias.get(matchId);
        if (copia != null && clock.instant().isBefore(copia.hasta())) return Optional.of(copia.mapa());
        Optional<MapaIsometrico> guardado = Optional.ofNullable(estado.redis().opsForValue().get(clave(matchId)))
                .map(json -> estado.deJson(json, Guardado.class).restaurar());
        guardado.ifPresentOrElse(this::recordar, () -> copias.remove(matchId));
        return guardado;
    }

    @Override
    public MapaIsometrico saveIfAbsent(MapaIsometrico mapa) {
        boolean nuevo = Boolean.TRUE.equals(estado.redis().opsForValue()
                .setIfAbsent(clave(mapa.getMatchId()), estado.aJson(Guardado.de(mapa)), estado.vida()));
        if (nuevo) {
            recordar(mapa);
            return mapa;
        }
        return findByMatchId(mapa.getMatchId()).orElse(mapa);
    }

    /** Borra la copia local de esta instancia. */
    public void olvidar(String matchId) {
        copias.remove(matchId);
    }

    boolean tieneCopia(String matchId) {
        return copias.containsKey(matchId);
    }

    private void recordar(MapaIsometrico mapa) {
        copias.put(mapa.getMatchId(), new EnCopia(mapa, clock.instant().plus(estado.vida())));
    }
}
