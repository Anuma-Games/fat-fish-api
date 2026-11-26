// Load test of the PWA endpoints with k6 (U3 E3 of the integrator project).
// Simulates PWA visitors: 40 % ranking, 25 % profile, 20 % match history, 10 % player search
// and 5 % survey submissions, with a 0.5 to 1.5 s pause between actions.
//
//   k6 run perf/carga-pwa.js                                   -> "ci" scenario (20 users, 1 min)
//   k6 run -e ESCENARIO=carga perf/carga-pwa.js                 -> average load (50 users, 4 min)
//   k6 run -e ESCENARIO=estres perf/carga-pwa.js                -> stress (up to 400 users, 3.5 min)
//   k6 run -e BASE_URL=https://... -e API_KEY=... -e SOLO_LECTURA=1 perf/carga-pwa.js
//
// The thresholds are the goals of the performance report: if one fails, k6 exits with a
// non-zero code and the CI job fails. The job only blocks a merge if it is a required check of the branch.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || '';
const SOLO_LECTURA = __ENV.SOLO_LECTURA === '1'; // production: never write
const JUGADORES = Number(__ENV.JUGADORES || 28); // demo data: players 1 to 28

const ESCENARIOS = {
  ci: { executor: 'constant-vus', vus: 20, duration: '1m' },
  carga: {
    executor: 'ramping-vus',
    stages: [
      { duration: '30s', target: 50 },
      { duration: '3m', target: 50 },
      { duration: '30s', target: 0 },
    ],
  },
  estres: {
    executor: 'ramping-vus',
    stages: [
      { duration: '30s', target: 100 },
      { duration: '1m', target: 200 },
      { duration: '1m', target: 400 },
      { duration: '30s', target: 400 },
      { duration: '30s', target: 0 },
    ],
  },
};

// A typo in ESCENARIO would leave the scenario undefined and k6 would fail with an unclear error
const ESCENARIO = __ENV.ESCENARIO || 'ci';
if (!ESCENARIOS[ESCENARIO]) {
  throw new Error(`Escenario desconocido "${ESCENARIO}". Usa uno de: ${Object.keys(ESCENARIOS).join(', ')}`);
}

// Outside a local API (the CI runner or docker compose) the script only reads and never runs "estres":
// a forgotten SOLO_LECTURA would write fake surveys to Cloud SQL, and 400 users would take down the
// single Cloud Run instance of the public demo
const HOSTS_LOCALES = ['localhost', '127.0.0.1', '[::1]', 'host.docker.internal', 'fatfish-api'];
// Credentials in the URL (http://localhost:x@host) would hide the real host from the check below
const AUTORIDAD = (BASE_URL.match(/^[a-z]+:\/\/([^/?#]*)/i) || [])[1] || '';
if (AUTORIDAD.includes('@')) {
  throw new Error('BASE_URL no debe incluir credenciales; usa -e API_KEY=...');
}
const HOST = (AUTORIDAD.match(/^(\[[^\]]+\]|[^:]+)/) || [])[1] || '';
if (!HOSTS_LOCALES.includes(HOST.toLowerCase())) {
  if (!SOLO_LECTURA) {
    throw new Error(`${BASE_URL} no es una API local: agrega -e SOLO_LECTURA=1 para no escribir encuestas`);
  }
  if (ESCENARIO === 'estres') {
    throw new Error(`El escenario "estres" solo se corre contra una API local, no contra ${BASE_URL}`);
  }
}

export const options = {
  scenarios: { pwa: ESCENARIOS[ESCENARIO] },
  // The global percentiles mix all routes: survey submissions are 5 % of the traffic, so a slow survey
  // endpoint would hide under them. Each route also gets its own p95 (p99 needs more samples per route)
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{endpoint:ranking}': ['p(95)<500'],
    'http_req_duration{endpoint:perfil}': ['p(95)<500'],
    'http_req_duration{endpoint:partidas}': ['p(95)<500'],
    'http_req_duration{endpoint:jugadores}': ['p(95)<500'],
    'http_req_duration{endpoint:encuesta}': ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

const headers = { 'X-Api-Key': API_KEY };
const randomPlayer = () => 1 + Math.floor(Math.random() * JUGADORES);
const ALIAS = ['an', 'el', 'ra', 'jo', 'ma'];

export default function () {
  const r = Math.random();
  let res;
  if (r < 0.4) {
    res = http.get(`${BASE_URL}/api/pwa/ranking?pagina=${Math.floor(Math.random() * 3)}`, { headers, tags: { endpoint: 'ranking' } });
  } else if (r < 0.65) {
    res = http.get(`${BASE_URL}/api/pwa/perfil?id=${randomPlayer()}`, { headers, tags: { endpoint: 'perfil' } });
  } else if (r < 0.85) {
    res = http.get(`${BASE_URL}/api/pwa/partidas?id=${randomPlayer()}`, { headers, tags: { endpoint: 'partidas' } });
  } else if (r < 0.95 || SOLO_LECTURA) {
    const q = ALIAS[Math.floor(Math.random() * ALIAS.length)];
    res = http.get(`${BASE_URL}/api/pwa/jugadores?busqueda=${q}`, { headers, tags: { endpoint: 'jugadores' } });
  } else {
    const body = JSON.stringify({ percepcionRiesgo: 4, cambioOpinion: 3, claridadMensaje: 5, comentario: 'Prueba de carga' });
    res = http.post(`${BASE_URL}/api/pwa/encuesta`, body, {
      headers: { ...headers, 'Content-Type': 'application/json' },
      tags: { endpoint: 'encuesta' },
    });
  }
  check(res, { 'respuesta 2xx': (x) => x.status >= 200 && x.status < 300 });
  sleep(0.5 + Math.random());
}
