package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.ElementoInteractivo;
import co.eci.c15.gameplay.domain.ElementoInteractivoRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Carga al arrancar los acertijos de ejemplo de los componentes acertijo-1..3 del mapa,
 * solo si todavía no existen. Reemplazar por el contenido real cuando esté listo.
 */
@Component
public class AcertijosPlaceholderLoader implements ApplicationRunner {

    private record Placeholder(String componente, String enunciado, List<String[]> elementos) {}

    private static final List<Placeholder> ACERTIJOS = List.of(
            new Placeholder("acertijo-1",
                    "Encuentra el número que falta en la secuencia: 2, 4, 8, ?, 32.",
                    List.<String[]>of(new String[]{"campo-numero", "Respuesta numérica"})),
            new Placeholder("acertijo-2",
                    "Ordena los colores del arcoíris empezando por el rojo.",
                    List.<String[]>of(new String[]{"lista-ordenable", "Colores a ordenar"})),
            new Placeholder("acertijo-3",
                    "Descifra la palabra: cada letra se corrió una posición en el alfabeto (FTDBQF).",
                    List.<String[]>of(new String[]{"campo-texto", "Palabra descifrada"})));

    private final AcertijoRepository acertijos;
    private final ElementoInteractivoRepository elementos;

    public AcertijosPlaceholderLoader(AcertijoRepository acertijos, ElementoInteractivoRepository elementos) {
        this.acertijos = acertijos;
        this.elementos = elementos;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Placeholder p : ACERTIJOS) {
            if (acertijos.findByComponenteMapaId(p.componente()).isPresent()) continue;
            String acertijoId = "placeholder-" + p.componente();
            acertijos.save(new Acertijo(acertijoId, p.componente(), p.enunciado()));
            for (int i = 0; i < p.elementos().size(); i++) {
                String[] e = p.elementos().get(i);
                elementos.save(new ElementoInteractivo(acertijoId + "-elemento-" + (i + 1), acertijoId, e[0], e[1]));
            }
        }
    }
}
