# Fat Fish API

[![CI](https://github.com/andrikesquer/fat-fish-api/actions/workflows/ci.yml/badge.svg)](https://github.com/andrikesquer/fat-fish-api/actions/workflows/ci.yml)

Prototipo del backend del videojuego **Fat Fish** (proyecto integrador, grupo IDGS101N, UTCH).

El juego guarda cada partida en el equipo del jugador (local first) y la sincroniza con esta API
cuando hay conexión. La API recibe lotes de partidas de forma idempotente y expone el historial y
las estadísticas de cada jugador para la PWA y la app móvil.

## Tecnologías

- Java 21, Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator)
- PostgreSQL 17 (local con Docker Compose) y migraciones con Flyway
- OpenAPI / Swagger UI (springdoc-openapi)
- JUnit 5, Mockito, MockMvc y pruebas de integración contra PostgreSQL real
- GitHub Actions (CI) y Dockerfile multi-stage

## Estructura

```
src/main/java/com/fatfish/api
├── controller   # Endpoints REST (/api/v1)
├── service      # Reglas de negocio: validación, idempotencia, estadísticas
├── repository   # Spring Data JPA
├── model        # Entidades JPA
├── dto          # Contrato JSON (RunRecord, respuestas)
├── exception    # Manejo de errores (Problem Details, mensajes en español)
└── config       # Configuración de OpenAPI
src/main/resources/db/migration   # Migraciones de Flyway (V1__initial_schema.sql)
.github/workflows/ci.yml          # Pipeline de CI
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
| Unitarias (Mockito) | `RunServiceTest`, `PlayerServiceTest` | Validaciones, idempotencia, cálculo de estadísticas |
| Controller (MockMvc) | `RunControllerTest`, `PlayerControllerTest` | Códigos HTTP, JSON y mensajes de error |
| Integración (PostgreSQL) | `RunApiIntegrationTest` | Flujo completo, mismo lote enviado dos veces sin duplicados, JSONB, paginación |

Para generar el `.jar` (queda en `target/fat-fish-api.jar`):

```bash
./mvnw package -DskipTests
java -jar target/fat-fish-api.jar
```

## Endpoints

Prefijo: `/api/v1`. Los errores siguen el formato Problem Details (RFC 9457) con mensajes en español.

### `POST /api/v1/runs`

Recibe un lote (máximo 100) de partidas. Es **idempotente por `runId`**: reenviar un lote no duplica nada.
El lote va dentro de un objeto (`runs`) porque `JsonUtility` de Unity no serializa arreglos en la raíz.

```json
{
  "runs": [
    {
      "schemaVersion": 1, "runId": "7d3f2a9e-1c4b-4f6e-9a8d-2b5c6e7f8a90",
      "installationId": "0e8f7c6d-5b4a-4c3d-8e2f-1a0b9c8d7e6f", "playerId": null,
      "gameVersion": "0.3.0", "startedAt": "2026-10-05T18:20:11Z", "endedAt": "2026-10-05T18:41:52Z",
      "result": "defeat", "levelReached": 2, "roundsCleared": 4, "bossesDefeated": 1,
      "maxSpinScore": 144, "totalSpins": 17, "totalBet": 2350, "totalWon": 1800, "totalLost": 1450,
      "allInCount": 1, "pawnedAssets": ["car"], "loansTaken": 1, "loanTotal": 500,
      "mostDesperateSpin": { "level": 2, "round": 3, "spin": 4, "bet": 900, "moneyBefore": 900, "allIn": true },
      "itemsBought": ["fresh_paint", "glass_chip"]
    }
  ]
}
```

Respuesta:

```json
{
  "accepted": ["7d3f2a9e-1c4b-4f6e-9a8d-2b5c6e7f8a90"],
  "duplicates": [],
  "rejected": [
    { "index": 1, "runId": "…", "reasons": ["levelReached debe estar entre 1 y 3"] }
  ]
}
```

Validaciones por partida: `levelReached` entre 1 y 3, `totalSpins` y montos no negativos,
`endedAt` posterior a `startedAt`, `result` en `defeat`, `demo_victory` o `abandoned`, `schemaVersion` = 1.
Una partida inválida no rechaza el lote completo.

### `GET /api/v1/players/{id}/runs?page=0&size=20`

Historial paginado (la página empieza en 0, `size` máximo 100), de la partida más reciente a la más antigua.

### `GET /api/v1/players/{id}/stats`

```json
{
  "playerId": "…", "runsPlayed": 2, "maxLevelReached": 3, "maxSpinScore": 210,
  "totalBet": 6350, "totalWon": 7000, "totalLost": 2350
}
```

## Modelo de datos

| Tabla | Descripción |
|---|---|
| `players` | Jugadores |
| `installations` | Copias del juego; `player_id` es nulo hasta vincularla a un jugador |
| `runs` | Partidas; `run_id` (UUID generado por el juego) es la llave primaria. `pawned_assets` y `most_desperate_spin` son JSONB |
| `run_items` | Objetos comprados en cada partida, en orden |

**Decisiones provisionales del prototipo** (hasta tener autenticación y vinculación de instalaciones):

- Una instalación nueva se registra automáticamente al recibir su primera partida.
- Si una partida trae un `playerId` que no existe, el jugador se registra automáticamente.
- Si la partida no trae `playerId` pero la instalación ya está vinculada, se asigna al jugador de la instalación.

## Integración continua

El workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) corre en cada push a `main` y `develop`
y en cada Pull Request hacia `main`:

1. Levanta un servicio `postgres:16` con healthcheck.
2. Configura Java 21 (Temurin) con caché de Maven.
3. `./mvnw test` (unitarias, controller e integración).
4. `./mvnw package -DskipTests`.
5. Sube como artefactos el `.jar` y los reportes de Surefire (estos últimos siempre, aunque fallen las pruebas).

## Equipo

Proyecto integrador IDGS101N, Universidad Tecnológica de Chihuahua.
