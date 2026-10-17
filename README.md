# Fat Fish API

[![CI](https://github.com/Anuma-Games/fat-fish-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Anuma-Games/fat-fish-api/actions/workflows/ci.yml)

Prototipo del backend del videojuego **Fat Fish** (proyecto integrador, grupo IDGS101N, UTCH).

El juego registra al jugador, guarda cada partida en su equipo (local first) y la envía a esta API
cuando hay conexión. La API guarda cada partida una sola vez, actualiza las estadísticas del jugador
y expone su perfil e historial para la PWA y la app móvil.

## Tecnologías

- Java 21, Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator)
- PostgreSQL 17 (local con Docker Compose) y migraciones con Flyway
- OpenAPI / Swagger UI (springdoc-openapi)
- JUnit 5, Mockito, MockMvc y pruebas de integración contra PostgreSQL real
- GitHub Actions (CI) y Dockerfile multi-stage

## Estructura

```
src/main/java/com/fatfish/api
├── controller   # Endpoints REST (/api/juego y /api/pwa)
├── service      # Reglas de negocio: token, idempotencia, estadísticas
├── repository   # Spring Data JPA
├── model        # Entidades JPA
├── dto          # Contrato JSON del juego (dto/juego) y de la PWA (dto/pwa)
├── exception    # Manejo de errores (Problem Details, mensajes en español)
└── config       # Configuración de OpenAPI
src/main/resources/db/migration   # Migraciones de Flyway (V1__esquema_inicial.sql)
.github/workflows/ci.yml          # Pipeline de CI
.github/scripts/test_summary.py   # Resumen de pruebas para GitHub Actions
```

## Requisitos

- JDK 21
- Docker Desktop (o Docker Engine con Compose)
- No hace falta instalar Maven: el proyecto incluye Maven Wrapper (`./mvnw`).

## 1. Levantar PostgreSQL con Docker Compose

```bash
docker compose up -d
```

Crea el contenedor `fatfish-postgres` con:

| Variable | Valor por defecto |
|---|---|
| Base de datos | `fatfish` (y `fatfish_test` para las pruebas) |
| `DB_USER` | `fatfish` |
| `DB_PASSWORD` | `fatfish` |
| `DB_PORT` | `5432` |

Los valores se pueden cambiar con variables de entorno o con un archivo `.env` (no se sube al repositorio).
Si ya tienes otro PostgreSQL en el puerto 5432, usa otro puerto:

```bash
DB_PORT=5433 docker compose up -d
```

> La base `fatfish_test` se crea solo la primera vez que arranca el volumen. Si ya tenías el volumen,
> bórralo con `docker compose down -v` (se pierden los datos locales) y vuelve a levantarlo.

## 2. Correr la aplicación

```bash
./mvnw spring-boot:run
```

La API lee la conexión de `DB_URL`, `DB_USER` y `DB_PASSWORD`
(por defecto `jdbc:postgresql://localhost:5432/fatfish`). Si cambiaste el puerto:

```bash
DB_URL=jdbc:postgresql://localhost:5433/fatfish ./mvnw spring-boot:run
```

Flyway aplica las migraciones al arrancar.

> **Si tu base local se creó antes de FF-51** (esquema con `players` y `runs`), la API no arranca porque la migración
> `V1` cambió. Borra y vuelve a crear las bases: con Docker, `docker compose down -v` y `docker compose up -d`; con
> PostgreSQL instalado, `dropdb fatfish` y `createdb -O fatfish fatfish` (igual con `fatfish_test`).

**Perfil `lab` (laboratorio de pruebas de la PWA):** agrega una línea de log por petición (método, ruta, código,
tiempo, tamaños y memoria), el SQL con sus parámetros y las métricas JDBC de cada transacción, en consola y en
`logs/fat-fish-api-lab.log`. Los pasos completos del laboratorio están en el repositorio
[`fat-fish-pwa`](https://github.com/Anuma-Games/fat-fish-pwa/tree/main/docs/laboratorio-pruebas).

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=lab
```

También puedes levantar la API y la base de datos en contenedores:

```bash
docker compose --profile app up -d --build
```

## 3. Documentación (Swagger)

Con la aplicación corriendo:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI (JSON): <http://localhost:8080/v3/api-docs>
- Salud: <http://localhost:8080/actuator/health>

## 4. Correr las pruebas

Las pruebas de integración usan el perfil `test`, que se conecta a la base `fatfish_test`.
Con Postgres levantado (paso 1):

```bash
./mvnw test
```

Si Postgres está en otro puerto:

```bash
DB_URL=jdbc:postgresql://localhost:5433/fatfish_test ./mvnw test
```

| Tipo | Clases | Qué cubren |
|---|---|---|
| Controller (MockMvc) | `GameControllerTest`, `PwaControllerTest` | Códigos HTTP, JSON, validaciones y mensajes de error |
| Integración (PostgreSQL) | `GameApiIntegrationTest`, `PwaApiIntegrationTest` | Registro y token, partida reenviada sin duplicados, estadísticas, comodines, perfil, historial y retroalimentación |

Para generar el `.jar` (queda en `target/fat-fish-api.jar`):

```bash
./mvnw package -DskipTests
java -jar target/fat-fish-api.jar
```

## Endpoints

Los errores siguen el formato Problem Details (RFC 9457) con mensajes en español. Los `id` viajan como texto.

### `POST /api/juego/registro`

Crea el jugador y sus estadísticas en cero. `alias`: de 3 a 24 caracteres (se puede repetir).

```json
{ "alias": "andrik" }
```

Responde `201`:

```json
{ "jugadorId": "5", "token": "q3Zt…" }
```

El token solo se entrega aquí. El juego lo guarda en el dispositivo y lo envía en la cabecera
`Authorization: Bearer <token>`; la base de datos solo guarda su SHA-256 (`jugador.token_hash`).

### `POST /api/juego/partidas`

Requiere `Authorization: Bearer <token>`; sin token o con uno que no existe responde `401`.

```json
{
  "numeroLocal": 3, "resultado": "DERROTA", "rondaAlcanzada": 4, "puntaje": 281,
  "dineroInvertido": 2350, "dineroGanado": 1800, "dineroPerdido": 1450,
  "vecesTodoONada": 1, "empenos": 0, "prestamos": 1, "duracionSeg": 1301,
  "comodines": [
    { "comodinId": 1, "vecesComprado": 2, "vecesUsado": 5 },
    { "comodinId": 3, "vecesComprado": 1, "vecesUsado": 0 }
  ]
}
```

- `numeroLocal`: contador de partidas que lleva el juego (1, 2, 3…). Es único por jugador: si el juego reenvía
  una partida (por ejemplo, porque se perdió la respuesta), la API responde `200` con la partida guardada y no
  inserta otra. La primera vez responde `201` con `{ "id": "12", "numeroLocal": 3, "creadaEn": "…" }`.
- `resultado`: `VICTORIA`, `DERROTA` o `ABANDONO`; `rondaAlcanzada`: de 1 a 9; montos y contadores no negativos.
- `comodines` es opcional. `comodinId` es el `id` de la tabla `comodin` (1 a 4) y no se puede repetir.
- Al guardar la partida, la API recalcula `estadistica_jugador` con todas las partidas del jugador en el orden de
  `numeroLocal`, así la racha de derrotas es correcta aunque una partida jugada sin conexión llegue tarde.

### `GET /api/pwa/perfil?id=1`

Alias, fechas y las seis estadísticas del expediente (partidas, ganadas, invertido, perdido, racha máxima de
derrotas y tiempo jugado), leídas de `estadistica_jugador`.

### `GET /api/pwa/partidas?id=1&pagina=0&tamano=10`

Historial del jugador para la PWA, con nombres de campo en español (`tamano` máximo 50). `id` es el `id` de la tabla
`jugador`. `generadoEn` indica cuándo
se generó la respuesta, para mostrar la antigüedad de los datos cuando la PWA los sirve desde la caché sin conexión.

```json
{
  "contenido": [
    { "id": "8", "resultado": "VICTORIA", "rondaAlcanzada": 9, "puntaje": 1510,
      "dineroInvertido": 9100, "dineroGanado": 11200, "dineroPerdido": 1900, "duracionSeg": 2645,
      "creadaEn": "2026-10-13T22:00:00Z" }
  ],
  "pagina": 0, "tamano": 10, "totalElementos": 8, "totalPaginas": 1, "ultima": true,
  "generadoEn": "2026-10-15T00:43:05Z"
}
```

### `POST /api/pwa/retroalimentacion`

Guarda una retroalimentación anónima. `categoria`: `ERROR`, `SUGERENCIA` u `OTRO`; `componente`: `JUEGO`, `PWA`
u `OTRO`; `mensaje`: de 10 a 1000 caracteres. Responde `201` con `{ "id": "5", "creadaEn": "…" }`.

La cabecera opcional `Idempotency-Key` (UUID) evita duplicados: la PWA la genera al guardar el envío y la repite en
cada reintento; si la clave ya existe, la API responde `200` con el registro guardado y no inserta otro.

## Modelo de datos

Una sola migración (`V1__esquema_inicial.sql`) crea todas las tablas y carga el catálogo de comodines.

| Tabla | Descripción |
|---|---|
| `jugador` | Jugadores registrados desde el juego; `token_hash` es el SHA-256 de su token |
| `estadistica_jugador` | Totales de cada jugador (1:1 con `jugador`); el ranking se ordena por `puntaje_maximo` y desempata con `puntaje_maximo_en` |
| `partida` | Partidas terminadas; `(jugador_id, numero_local)` es único |
| `comodin` | Catálogo de los 4 comodines del MVP |
| `partida_comodin` | Veces que se compró y usó cada comodín en una partida |
| `retroalimentacion` | Retroalimentación anónima de la PWA; `clave_idempotencia` es única |
| `encuesta` | Encuesta anónima de la PWA; `clave_idempotencia` es única (sin endpoint todavía) |

## Integración continua

El workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) corre en cada push a `main` y `develop`
y en cada Pull Request hacia `main` o `develop`. Tiene dos jobs:

**Compilar y probar**

1. Levanta un servicio `postgres:17` con healthcheck (misma versión que en local).
2. Configura Java 21 (Temurin) con caché de Maven.
3. `./mvnw test` (unitarias, controller e integración).
4. Publica en el *Summary* de la ejecución una tabla con los resultados de las pruebas
   ([`.github/scripts/test_summary.py`](.github/scripts/test_summary.py)), aunque fallen.
5. `./mvnw package -DskipTests`.
6. Sube como artefactos el `.jar` y los reportes de Surefire (estos últimos siempre, aunque fallen las pruebas).

**Construir imagen Docker** (solo si el job anterior pasa)

1. Construye la imagen con el `Dockerfile` multi-stage (todavía no se publica en un registro).
2. Publica en el *Summary* el nombre y tamaño de la imagen.

Los artefactos se descargan desde la página *Summary* de cada ejecución en la pestaña **Actions**
(sección *Artifacts*) o con `gh run download`.

### Flujo de ramas

- `main`: producción. `develop`: integración. Cambios nuevos en `feature/<clave-jira>-<descripcion>`.
- `main` y `develop` están protegidas (también para administradores):
  - Solo se fusiona por Pull Request, con **una aprobación** de otro miembro del equipo.
  - Los checks *Compilar y probar* y *Construir imagen Docker* deben estar en verde y la rama al día.
  - Las conversaciones del Pull Request deben quedar resueltas.
  - No se permite push directo, force push ni borrar la rama.
- `CODEOWNERS` asigna revisores automáticamente y la plantilla de PR pide el ticket de Jira y cómo probar el cambio.
- Dependabot revisa cada semana las dependencias (Maven, acciones de GitHub e imagen de Docker) y abre PRs hacia `develop`.

## Equipo

**Anuma Games**, Proyecto integrador, grupo IDGS101N, Universidad Tecnológica de Chihuahua.

| Integrante |
|---|
| Luis Ángel Gaxiola Gamboa |
| Emmanuel González Hernández |
| Jesús Antonio Jiménez Mendoza |
| Ángel Gael Jurado Rodríguez |
| Andrik Rivera Esquer |
