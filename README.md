# c15-gameplay

Servicio de **gameplay** del EscapeRoom C15 (ARSW, equipo C15-PortalSuba):
- mapa, movimiento y posiciones;
- acertijos (bloqueo, clicks, solución);
- inventario y roles Héroe/Verdugo;
- progreso;
- cronómetro;
- reto Héroe vs Verdugo;
- resultado.

Sale del monolito [ScapeRoomC15](https://github.com/JuanLopezTW/ScapeRoomC15), que se queda desplegado como respaldo.

## Qué hace

- **API:**
  - `/api/partidas/{matchId}/...`: mapa, jugadores, movimiento, acertijos, objetos, inventario, roles, progreso, cronómetro, reto y resultado.
  - Desarrollo: `POST /dev/partidas/{m}/inicio?segundos=` y `POST /dev/partidas/{m}/reto/inicio`.
- **Escucha** de Kafka:
  - `c15.partida-iniciada`, que viene de salas: arma la partida con los equipos y la duración (5, 10 o 15 min).
  - `c15.partida-terminada`: libera el estado.
  - `c15.sesion`, que viene del broker: al cerrarse la conexión del mapa, el jugador sale.
- **Publica** en Kafka `c15.partida-terminada`, con el ganador, los puntajes y los miembros de cada equipo. Lo usan salas y social.
- **Publica** en los topics STOMP `/topic/partidas/{m}/...` por RabbitMQ. No tiene WebSocket propio: los jugadores se conectan a `c15-broker`, que autoriza los topics de equipo.
- **Varias réplicas:** con `ESTADO_REDIS=true`, el estado de la partida vive en Redis y cada partida tiene un dueño (turno de 10 s) que lleva el cronómetro y el reto.
- **Seguridad:** valida el JWT de Auth0 en toda la API salvo `/api/version`. El `userId` es el `sub` del token.

`co.eci.c15.plataforma` (seguridad, observabilidad, Kafka, STOMP) es una copia de lo que tienen todos los
servicios. Si se cambia aquí, hay que cambiarlo también en los demás.

## Correr

```bash
mvn verify                  # pruebas + cobertura (minimo 80%); las de Kafka y Redis necesitan Docker
mvn spring-boot:run         # necesita Postgres, Kafka, RabbitMQ (y Redis si ESTADO_REDIS=true): ver .env.example
```

Para levantar todo el sistema junto, usa el compose de `c15-gateway`.
