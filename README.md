# Fat Fish – API del videojuego

[![CI](https://github.com/Anuma-Games/fat-fish-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Anuma-Games/fat-fish-api/actions/workflows/ci.yml)

<!--suppress HtmlDeprecatedAttribute --><p align="center"><img src="docs/img/logo.png" alt="Logo de Fat Fish" width="160"></p>

Backend del proyecto integrador **Fat Fish**, del equipo Anuma Games (IDGS101N, Universidad Tecnológica de Chihuahua).
La consumen el [juego](https://github.com/Anuma-Games/fat-fish-game),
la [PWA](https://github.com/Anuma-Games/fat-fish-pwa)
y la [app móvil](https://github.com/Anuma-Games/fat-fish-mobile).

El juego registra al jugador, guarda cada partida en su equipo (local first) y la envía a esta API
cuando hay conexión. La API guarda cada partida una sola vez, actualiza las estadísticas del jugador
y expone su perfil, historial, ranking mundial y búsqueda de jugadores para la PWA y la app móvil, que también
envía retroalimentación y la encuesta anónima.

## Contenido

- [Tecnologías](#tecnologías)
- [Estructura](#estructura)
- [Requisitos](#requisitos)
- [Uso de la API en Cloud Run](#uso-de-la-api-en-cloud-run)
    - [Qué se necesita](#qué-se-necesita)
    - [Primera prueba](#primera-prueba)
    - [Errores](#errores)
    - [Seguridad: límites y bloqueo](#seguridad-límites-y-bloqueo)
    - [Configuración en Cloud Run](#configuración-en-cloud-run)
- [1. Levantar PostgreSQL con Docker Compose](#1-levantar-postgresql-con-docker-compose)
- [2. Correr la aplicación](#2-correr-la-aplicación)
- [3. Documentación (Swagger)](#3-documentación-swagger)
- [4. Correr las pruebas](#4-correr-las-pruebas)
- [5. Datos de demostración](#5-datos-de-demostración)
- [Endpoints](#endpoints)
- [Modelo de datos](#modelo-de-datos)
- [Integración continua](#integración-continua)
- [Equipo](#equipo)

---

## Tecnologías

- Java 21, Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator) y `spring-security-crypto` (BCrypt)
- PostgreSQL 17 (local con Docker Compose) y migraciones con Flyway
- OpenAPI / Swagger UI (springdoc-openapi)
- JUnit 5, Mockito, MockMvc y pruebas de integración contra PostgreSQL real
- GitHub Actions (CI) y Dockerfile multi-stage

## Estructura

```
src/main/java/com/fatfish/api
├── controller   # Endpoints REST (/api/juego y /api/pwa)
├── service      # Reglas de negocio: token, idempotencia, estadísticas, bloqueo de sesión
├── repository   # Spring Data JPA
├── model        # Entidades JPA
├── dto          # Contrato JSON del juego (dto/juego) y de la PWA (dto/pwa)
├── exception    # Manejo de errores (Problem Details, mensajes en español)
└── config       # OpenAPI, llave de la API y límite de peticiones
src/main/resources/db/migration   # Migraciones de Flyway (V1__esquema_inicial.sql, V2__…)
sql/demo                          # Datos de demostración (se cargan a mano, no con Flyway)
docs/diagramas/modelo-datos.mmd   # Diagrama del modelo de datos (copia de referencia)
.github/workflows/ci.yml          # Pipeline de CI
.github/scripts/test_summary.py   # Resumen de pruebas para GitHub Actions
```

## Requisitos

- JDK 21
- Docker Desktop (o Docker Engine con Compose)
- No hace falta instalar Maven: el proyecto incluye Maven Wrapper (`./mvnw`).

## Uso de la API en Cloud Run

**URL base (pruebas):** `https://fat-fish-api-31376020434.us-south1.run.app` (solo HTTPS).
Documentación en `/swagger-ui.html` y salud en `/actuator/health`. Los clientes leen la URL de un archivo de
configuración, no la escriben en el código.

### Qué se necesita

| Control               | Qué es                                                                                                                                                    | Cabecera                        |
|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| **Llave de la API**   | Identifica a la aplicación que llama. **Es pública por diseño** (la PWA la envía desde el navegador); se pide al responsable de la API y no se sube a Git | `X-Api-Key: <llave>`            |
| **Token del jugador** | Lo entrega `registro` o `sesion` y autoriza escribir partidas                                                                                             | `Authorization: Bearer <token>` |

| Endpoint                           | Llave |        Token         | Éxito                                       | Errores             |
|------------------------------------|:-----:|:--------------------:|---------------------------------------------|---------------------|
| `POST /api/juego/registro`         |  Sí   |          No          | `201`                                       | `400`, `429`        |
| `POST /api/juego/sesion`           |  Sí   | No (id y contraseña) | `200`                                       | `400`, `401`, `429` |
| `PUT /api/juego/contrasena`        |  Sí   |          Sí          | `204`                                       | `400`, `401`        |
| `POST /api/juego/partidas`         |  Sí   |          Sí          | `201` (`200` si ya existía)                 | `400`, `401`        |
| `GET /api/pwa/jugadores?busqueda=` |  Sí   |          No          | `200` (lista vacía si no hay coincidencias) | `400`               |
| `GET /api/pwa/perfil?id=`          |  Sí   |          No          | `200`                                       | `400`, `404`        |
| `GET /api/pwa/partidas?id=`        |  Sí   |          No          | `200`                                       | `400`, `404`        |
| `GET /api/pwa/ranking`             |  Sí   |          No          | `200`                                       | `400`               |
| `POST /api/pwa/retroalimentacion`  |  Sí   |          No          | `201` (`200` si ya existía)                 | `400`, `429`        |
| `POST /api/pwa/encuesta`           |  Sí   |          No          | `201` (`200` si ya existía)                 | `400`, `429`        |
| `GET /actuator/health`             |  No   |          No          | `200`                                       |                     |

### Primera prueba

```bash
URL=https://fat-fish-api-31376020434.us-south1.run.app
KEY='<llave-del-equipo>'

curl -i $URL/actuator/health                                   # 200, sin llave
curl -i "$URL/api/pwa/perfil?id=1"                             # 403: falta la llave
curl -i -H "X-Api-Key: $KEY" "$URL/api/pwa/perfil?id=1"        # 200, o 404 si no existe
```

En PowerShell usa `curl.exe`. En Postman, define `X-Api-Key: {{apiKey}}` a nivel de colección.

### Errores

Formato Problem Details (RFC 9457), en español.

| Código      | Qué hacer                                                                                                                                                    |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `400`       | Corregir el cuerpo (`errors` lista los campos). No reintentar igual                                                                                          |
| `401`       | Token ausente, inválido o reemplazado: pedir `POST /api/juego/sesion`. En `sesion`, contraseña incorrecta o jugador bloqueado (misma respuesta, a propósito) |
| `403`       | Falta la llave o es inválida. **No pedir al jugador iniciar sesión**                                                                                         |
| `404`       | El jugador no existe                                                                                                                                         |
| `429`       | Límite de peticiones de la IP. Esperar los segundos de `Retry-After` y reintentar                                                                            |
| `5xx` o red | Reintentar con espera creciente y la misma clave de idempotencia                                                                                             |

### Seguridad: límites y bloqueo

La llave no protege los datos. Los protege el token del jugador y, en los endpoints sin token, estas dos medidas:

| Medida                                      | Dónde                             | Valor por defecto                          | Propiedad                                                  |
|---------------------------------------------|-----------------------------------|--------------------------------------------|------------------------------------------------------------|
| Límite por IP (`RateLimitFilter`)           | `POST /api/juego/registro`        | 5 por hora                                 | `fatfish.security.rate-limit.register-per-hour`            |
|                                             | `POST /api/juego/sesion`          | 10 por minuto                              | `fatfish.security.rate-limit.login-per-minute`             |
|                                             | `POST /api/pwa/retroalimentacion` | 10 por minuto                              | `fatfish.security.rate-limit.feedback-per-minute`          |
|                                             | `POST /api/pwa/encuesta`          | 10 por minuto                              | `fatfish.security.rate-limit.survey-per-minute`            |
| Bloqueo por jugador (`LoginAttemptService`) | `POST /api/juego/sesion`          | 5 fallos en 15 minutos bloquean 15 minutos | `fatfish.security.login-lockout.max-attempts` y `.minutes` |

- Se encienden con `fatfish.security.rate-limit.enabled` y `fatfish.security.login-lockout.enabled`. Están apagados
  por defecto (local y CI) y encendidos en el perfil `cloudrun`.
- Al pasarse del límite responde `429` con `Retry-After`. Un jugador bloqueado recibe el mismo `401` que una
  contraseña incorrecta, aunque la contraseña sea correcta. Los ids que no existen también se cuentan.
- La IP es la **última** entrada de `X-Forwarded-For`: Cloud Run agrega ahí la IP real, y lo anterior lo puede
  escribir el cliente.
- Los contadores viven en memoria: se pierden al desplegar y no se comparten entre instancias. Cloud Run debe tener
  **máximo 1 instancia**.
- Cualquiera que conozca el id de un jugador puede bloquear su inicio de sesión 15 minutos. Se acepta en el MVP: el
  juego funciona sin sesión y el token del dispositivo sigue sirviendo.

### Configuración en Cloud Run

La API corre en Cloud Run con el perfil `cloudrun` y se conecta a Cloud SQL con el conector de Google, sin IPs
autorizadas. La contraseña de la base y la llave de la API se guardan en Secret Manager y llegan como variables de
entorno; nunca se escriben en el repositorio.

| Variable                            | Uso                                                                                                                                                               |
|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`            | `cloudrun`: pool de conexiones pequeño, Flyway activo, URLs `https` en Swagger y apagado ordenado                                                                 |
| `DB_URL`                            | Conexión por el conector: `jdbc:postgresql:///fatfish?cloudSqlInstance=<proyecto>:<región>:<instancia>&socketFactory=com.google.cloud.sql.postgres.SocketFactory` |
| `DB_USER` y `DB_PASSWORD`           | Usuario y contraseña de la base (la contraseña, desde Secret Manager)                                                                                             |
| `FATFISH_SECURITY_API_KEY`          | Llave que se compara con la cabecera `X-Api-Key` (desde Secret Manager). Si está vacía, el filtro queda apagado                                                   |
| `FATFISH_SECURITY_API_KEY_REQUIRED` | Con `true`, la aplicación no arranca si falta la llave, para no quedar abierta por error                                                                          |

El perfil `cloudrun` es para la API publicada. Para trabajar desde tu computadora contra la base de la nube se usa el
perfil `gcp` (sección 2).

## 1. Levantar PostgreSQL con Docker Compose

```bash
docker compose up -d
```

Crea el contenedor `fatfish-postgres` con:

| Variable      | Valor por defecto                             |
|---------------|-----------------------------------------------|
| Base de datos | `fatfish` (y `fatfish_test` para las pruebas) |
| `DB_USER`     | `fatfish`                                     |
| `DB_PASSWORD` | `fatfish`                                     |
| `DB_PORT`     | `5432`                                        |

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

En local la llave de la API está apagada. Para probarla, arranca con una llave cualquiera; las peticiones a `/api/**`
sin la cabecera `X-Api-Key: mi-llave` responden `403`:

```bash
FATFISH_SECURITY_API_KEY=mi-llave ./mvnw spring-boot:run
```

El límite de peticiones y el bloqueo de sesión también están apagados en local. Para probarlos:

```bash
FATFISH_SECURITY_RATE_LIMIT_ENABLED=true FATFISH_SECURITY_LOGIN_LOCKOUT_ENABLED=true ./mvnw spring-boot:run
```

La IP se simula con `X-Forwarded-For`. La sexta petición de una IP responde `429`:

```bash
for i in 1 2 3 4 5 6; do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST localhost:8080/api/juego/registro \
    -H "X-Forwarded-For: 10.0.0.1" -H "Content-Type: application/json" \
    -d '{ "alias": "andrik", "contrasena": "pez-gordo-2026" }'
done
```

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

**Perfil `gcp` (base de pruebas en Google Cloud SQL):** se conecta con SSL a la base de la nube. Los datos de
conexión van en un archivo `.env` en la raíz del proyecto, que no se sube al repositorio (está en `.gitignore`):

```properties
DB_HOST=<IP pública de Cloud SQL>
DB_NAME=fatfish
DB_USER=<usuario>
DB_PASSWORD=<contraseña>
```

> `docker compose` también lee el `.env` de la raíz y tomaría `DB_USER` y `DB_PASSWORD` de Cloud SQL para crear el
> contenedor local. Si tienes este archivo, levanta Docker con `docker compose --env-file /dev/null up -d`.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=gcp
```

Al arrancar, Flyway aplica en la nube las migraciones pendientes (igual que en local) y Hibernate valida el esquema.
`/actuator/health` debe mostrar la base como `UP`. Como las migraciones que ya están en `main` no se editan, la base de
la nube nunca se borra: cada cambio de esquema llega con una migración nueva.

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

| Tipo                                       | Clases                                                               | Qué cubren                                                                                                         |
|--------------------------------------------|----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| Unitarias del servicio (JUnit 5 y Mockito) | `PwaServiceTest`                                                     | `findAllFeedback` y `createFeedback` de `PwaService`, con `FeedbackRepository` simulado                            |
| Controller (MockMvc)                       | `GameControllerTest`, `PwaControllerTest`                            | Códigos HTTP, JSON, validaciones y mensajes de error                                                               |
| Unitarias de seguridad                     | `ApiKeyFilterTest`, `RateLimitFilterTest`, `LoginAttemptServiceTest` | Llave de la API, límite por IP (incluye `X-Forwarded-For` falsificado) y bloqueo de sesión                         |
| Integración (PostgreSQL)                   | `GameApiIntegrationTest`, `PwaApiIntegrationTest`                    | Registro y token, partida reenviada sin duplicados, estadísticas, comodines, perfil, historial y retroalimentación |
| Integración (PostgreSQL)                   | `SecurityLimitsIntegrationTest`                                      | Con los límites encendidos: `429` en los cuatro endpoints sin token, bloqueo de `/sesion` y `/partidas` sin límite |

En `PwaService` los métodos de findAll y create se llaman `findAllFeedback()` y `createFeedback()`, no `findAll()` y
`save()` como en el ejemplo de la práctica. El servicio maneja varias tablas (jugadores, partidas, ranking,
retroalimentación y encuestas), y un `findAll()` sin más no diría qué lista devuelve. Las pruebas conservan los nombres
de la práctica: `testFindAllFeedback` y `testCreateFeedback`.

Ejecución de `mvn test` con las 48 pruebas en verde:

![Ejecución exitosa de mvn test](docs/pruebas/mvn-test.png)

Para generar el `.jar` (queda en `target/fat-fish-api.jar`):

```bash
./mvnw package -DskipTests
java -jar target/fat-fish-api.jar
```

## 5. Datos de demostración

`sql/demo` tiene un juego de datos ficticio para mostrar la PWA con contenido creíble: 28 jugadores, 107 partidas,
comodines, 6 retroalimentaciones y 8 encuestas. Es el mismo que usa el laboratorio de pruebas de la PWA: en el
repositorio `fat-fish-pwa`, `docs/laboratorio-pruebas/sql/02_carga.sql` es una copia de `02_cargar.sql`. Si cambias
uno, cambia el otro.

No son migraciones de Flyway: Flyway corre cada migración una sola vez y en todas las bases, incluida la de las
pruebas. Estos scripts se corren a mano con `psql` y solo donde se quieran los datos.

| Script           | Cuándo                                                                                                                                                                                                                              |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `01_limpiar.sql` | Borra solo los datos de demostración: id en su rango (jugador 1 a 28, retroalimentacion 1 a 6, encuesta 1 a 8) **y** su marca (el hash fijo de la contraseña o la fecha exacta). Los datos reales no se tocan aunque tengan esos id |
| `02_cargar.sql`  | Carga los datos de demostración; si ya están, falla sin cambiar nada (todo va en una transacción)                                                                                                                                   |

Las cuentas tienen una contraseña aleatoria que nadie conoce y tokens aleatorios, así que nadie puede iniciar sesión
con ellas ni enviar partidas a su nombre. Al final, cada secuencia se mueve al id más alto de su tabla: el siguiente
jugador que se registre será el 29. Si en una base ya hay jugadores reales con id del 1 al 28, `02_cargar.sql`
falla por id repetido y no cambia nada.

Con los datos del `.env` del perfil `gcp`:

```bash
set -a; source .env; set +a
export PGPASSWORD="$DB_PASSWORD"
psql "host=$DB_HOST dbname=$DB_NAME user=$DB_USER sslmode=require" -v ON_ERROR_STOP=1 -f sql/demo/01_limpiar.sql
psql "host=$DB_HOST dbname=$DB_NAME user=$DB_USER sslmode=require" -v ON_ERROR_STOP=1 -f sql/demo/02_cargar.sql
unset PGPASSWORD
```

En local son los mismos dos comandos con `psql -U fatfish -h localhost -d fatfish`. Cuando ya haya jugadores reales,
saca un respaldo de Cloud SQL antes de correr cualquiera de estos scripts.

## Endpoints

Los errores siguen el formato Problem Details (RFC 9457) con mensajes en español. Los `id` viajan como texto.

### `POST /api/juego/registro`

Crea el jugador y sus estadísticas en cero. `alias`: de 3 a 24 caracteres (se puede repetir);
`contrasena`: de 8 a 72 caracteres, se guarda con BCrypt (`jugador.contrasena_hash`).

```json
{
  "alias": "andrik",
  "contrasena": "pez-gordo-2026"
}
```

Responde `201`:

```json
{
  "jugadorId": "5",
  "token": "q3Zt…"
}
```

El token solo se entrega aquí. El juego lo guarda en el dispositivo y lo envía en la cabecera
`Authorization: Bearer <token>`; la base de datos solo guarda su SHA-256 (`jugador.token_hash`).

### `POST /api/juego/sesion`

Para usar la cuenta en otro dispositivo (o si el juego perdió el token). Con el id y la contraseña responde `200`
con un token nuevo, en el mismo formato que el registro; **el token anterior deja de funcionar**. Si el id no existe
o la contraseña es incorrecta responde `401` con el mismo mensaje, para no revelar qué ids existen. Lo mismo responde
si el jugador está bloqueado por fallos (ver [Seguridad](#seguridad-límites-y-bloqueo)).

```json
{
  "jugadorId": 5,
  "contrasena": "pez-gordo-2026"
}
```

### `PUT /api/juego/contrasena`

Requiere `Authorization: Bearer <token>`. Cambia la contraseña sin pedir la anterior: el token demuestra que es el
dueño de la cuenta, así que el jugador puede recuperarla desde el dispositivo donde juega. Responde `204`.

```json
{
  "contrasenaNueva": "otra-clave-segura"
}
```

Si el jugador pierde el dispositivo y también olvidó la contraseña, la cuenta no se puede recuperar.

### `POST /api/juego/partidas`

Requiere `Authorization: Bearer <token>`; sin token o con uno que no existe responde `401`.

```json
{
  "claveIdempotencia": "3f2c9a1e-7b4d-4c8a-9e21-5d0f6a8b7c13",
  "terminadaEn": "2026-10-16T17:40:00Z",
  "resultado": "DERROTA",
  "rondaAlcanzada": 4,
  "puntaje": 281,
  "dineroInvertido": 2350,
  "dineroGanado": 1800,
  "dineroPerdido": 1450,
  "vecesTodoONada": 1,
  "empenos": 0,
  "prestamos": 1,
  "duracionSeg": 1301,
  "comodines": [
    {
      "comodinId": 1,
      "vecesComprado": 2,
      "vecesUsado": 5
    },
    {
      "comodinId": 3,
      "vecesComprado": 1,
      "vecesUsado": 0
    }
  ]
}
```

- `claveIdempotencia`: el `runId` (UUID) que el juego genera con `System.Guid.NewGuid()` al terminar la partida y
  guarda en su JSON local. Si el juego reenvía la partida (por ejemplo, porque se perdió la respuesta), la API
  responde `200` con la partida guardada y no inserta otra. La primera vez responde `201` con
  `{ "id": "12", "claveIdempotencia": "…", "creadaEn": "…" }`. Como la clave es única por partida y no un contador,
  las partidas de una misma cuenta en varios dispositivos no se pisan.
- `terminadaEn`: fecha en que el juego terminó la partida; el historial de la PWA la muestra y la ordena con ella.
- `resultado`: `VICTORIA`, `DERROTA` o `ABANDONO`; `rondaAlcanzada`: 1 o más (sin límite superior, las rondas son
  infinitas); montos y contadores no negativos.
- `comodines` es opcional. `comodinId` es el `id` de la tabla `comodin` (1 a 4) y no se puede repetir.
- Al guardar la partida, la API recalcula `estadistica_jugador` con todas las partidas del jugador en el orden de
  `terminadaEn`, así la racha de derrotas es correcta aunque una partida jugada sin conexión llegue tarde.
- **Modo invitado:** el juego permite jugar sin cuenta; esas partidas solo se guardan en el dispositivo. Al crear la
  cuenta o iniciar sesión, el juego las envía a este endpoint con su clave, sin cambios en la API.

### `GET /api/pwa/jugadores?busqueda=andrik`

Busca jugadores para la PWA. `busqueda` es obligatoria (máximo 24 caracteres). Si el texto son solo dígitos, busca
primero el jugador con ese `id` exacto; después agrega los jugadores cuyo `alias` contenga el texto, sin distinguir
mayúsculas, ordenados por última actividad. Devuelve máximo 20 resultados sin repetir jugadores; si no hay
coincidencias responde `200` con una lista vacía.

```json
[
  {
    "jugadorId": "5",
    "alias": "andrik",
    "ultimaActividad": "2026-10-13T22:44:05Z"
  }
]
```

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
    {
      "id": "8",
      "resultado": "VICTORIA",
      "rondaAlcanzada": 9,
      "puntaje": 1510,
      "dineroInvertido": 9100,
      "dineroGanado": 11200,
      "dineroPerdido": 1900,
      "duracionSeg": 2645,
      "creadaEn": "2026-10-13T22:00:00Z"
    }
  ],
  "pagina": 0,
  "tamano": 10,
  "totalElementos": 8,
  "totalPaginas": 1,
  "ultima": true,
  "generadoEn": "2026-10-15T00:43:05Z"
}
```

### `GET /api/pwa/ranking?pagina=0&tamano=10&jugadorId=5`

Ranking mundial: el mejor puntaje en una victoria de cada jugador, hasta el lugar 50. Se desempata por quién lo logró
primero. `tamano` va de 1 a 10 y `pagina` empieza en 0; el número de páginas depende de `tamano` (5 páginas de 10
cubren el top 50) y una página fuera del top 50 responde vacía. `jugadorId` es opcional: si se envía, `miPosicion`
trae su lugar real en el ranking aunque quede fuera del top 50; es `null` si no se envió o el jugador todavía no
tiene victorias. `pagina` usa el mismo formato paginado que el historial de partidas.

```json
{
  "pagina": {
    "contenido": [
      {
        "posicion": 1,
        "jugadorId": "8",
        "alias": "gael",
        "puntaje": 1510,
        "logradoEn": "2026-10-13T22:00:00Z"
      }
    ],
    "pagina": 0,
    "tamano": 10,
    "totalElementos": 1,
    "totalPaginas": 1,
    "ultima": true,
    "generadoEn": "2026-10-15T00:43:05Z"
  },
  "miPosicion": {
    "posicion": 12,
    "jugadorId": "5",
    "alias": "andrik",
    "puntaje": 640,
    "logradoEn": "2026-10-09T18:30:00Z"
  }
}
```

### `POST /api/pwa/retroalimentacion`

Guarda una retroalimentación anónima. `categoria`: `ERROR`, `SUGERENCIA` u `OTRO`; `componente`: `JUEGO`, `PWA`
u `OTRO`; `mensaje`: de 10 a 1000 caracteres. Responde `201` con `{ "id": "5", "creadaEn": "…" }`.

La cabecera opcional `Idempotency-Key` (UUID) evita duplicados: la PWA la genera al guardar el envío y la repite en
cada reintento; si la clave ya existe, la API responde `200` con el registro guardado y no inserta otro.

### `POST /api/pwa/encuesta`

Guarda una encuesta anónima sobre la percepción del juego y la ludopatía. `percepcionRiesgo`, `cambioOpinion` y
`claridadMensaje`: obligatorios, de 1 a 5; `comentario`: opcional, máximo 500 caracteres (si viene vacío se guarda
como nulo). Responde `201`:

```json
{
  "id": "3",
  "created": true,
  "enviadaEn": "2026-10-16T20:00:00Z"
}
```

La cabecera opcional `Idempotency-Key` (UUID) funciona igual que en la retroalimentación: si la clave ya existe, la
API responde `200` con `created` en `false` y no inserta un duplicado. La encuesta no guarda ningún dato del jugador.

## Modelo de datos

`V1__esquema_inicial.sql` crea las tablas y carga el catálogo de comodines; `V2__partida_clave_idempotencia.sql`
cambia el contador local de `partida` por la clave UUID de cada partida y agrega `terminada_en`;
`V3__ronda_sin_limite.sql` cambia el check de `ronda_alcanzada` a `>= 1` para aceptar partidas que pasan de la ronda 9.

> **Las migraciones que ya están en `main` no se editan.** Flyway compara su contenido con el que ya aplicó y la API
> no arranca si cambió. Cualquier cambio al esquema va en una migración nueva (`V3__…`, `V4__…`), que Flyway aplica
> sola al arrancar sin borrar datos.

![Modelo de datos](docs/diagramas/modelo-datos.png)

<sub>Código del diagrama: [`docs/diagramas/modelo-datos.mmd`](docs/diagramas/modelo-datos.mmd). El uso de cada tabla
en la PWA está en su [README](https://github.com/Anuma-Games/fat-fish-pwa#5-modelo-de-datos);
si el esquema cambia, se actualizan las dos copias del diagrama.</sub>

| Tabla                 | Descripción                                                                                                                |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------|
| `jugador`             | Jugadores registrados desde el juego; `contrasena_hash` (BCrypt) y `token_hash` (SHA-256 del token) nunca salen en la API  |
| `estadistica_jugador` | Totales de cada jugador (1:1 con `jugador`); el ranking se ordena por `puntaje_maximo` y desempata con `puntaje_maximo_en` |
| `partida`             | Partidas terminadas; `clave_idempotencia` (UUID que genera el juego) es única y `terminada_en` es la fecha en que se jugó  |
| `comodin`             | Catálogo de los 4 comodines del MVP                                                                                        |
| `partida_comodin`     | Veces que se compró y usó cada comodín en una partida                                                                      |
| `retroalimentacion`   | Retroalimentación anónima de la PWA; `clave_idempotencia` es única                                                         |
| `encuesta`            | Encuesta anónima de la PWA; `clave_idempotencia` es única                                                                  |

## Integración continua

El workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) corre en cada push a `main` y `develop`
y en cada Pull Request hacia `main` o `develop`. Tiene dos jobs:

**Compilar y probar**

1. Levanta un servicio `postgres:17` con healthcheck (misma versión que en local).
2. Configura Java 21 (Temurin) con caché de Maven.
3. `./mvnw test` (unitarias, controller e integración).
4. Publica en el *Summary* de la ejecución una tabla con los resultados de las pruebas ([
   `.github/scripts/test_summary.py`](.github/scripts/test_summary.py)), aunque fallen.
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
- Dependabot revisa cada semana las dependencias (Maven, acciones de GitHub e imagen de Docker) y abre PRs hacia
  `develop`.

## Equipo

**Anuma Games** – Proyecto integrador, grupo IDGS101N, Universidad Tecnológica de Chihuahua.

- Luis Ángel Gaxiola Gamboa
- Emmanuel González Hernández
- Jesús Antonio Jiménez Mendoza
- Ángel Gael Jurado Rodríguez
- Andrik Rivera Esquer
