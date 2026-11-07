-- Fat Fish: datos de demostración (FF-129)
-- Mismo juego de datos para producción (Cloud SQL) y para el laboratorio de pruebas de la PWA.
-- No es una migración de Flyway: se corre a mano con psql, después de 01_limpiar.sql.
-- Todos los alias y el dinero son ficticios.
--
-- Ids fijos (jugador 1 a 28, partida 1 a 107, retroalimentacion 1 a 6, encuesta 1 a 8). 01_limpiar.sql los
-- reconoce por el id y por una marca: el hash de la contraseña de los jugadores y la fecha de los demás.
-- Si se corre dos veces sin limpiar, falla por id repetido y no carga nada (todo va en una transacción).

begin;

-- Contraseña: BCrypt de una contraseña aleatoria que nadie conoce, así nadie puede iniciar sesión
-- (POST /api/juego/sesion) con estas cuentas. Token: SHA-256 de un valor aleatorio, distinto en cada carga.
-- ultima_actividad es la fecha de su última partida; recien_llegado se registró y todavía no juega.
-- Hay dos jugadores con el alias Tiburon (6 y 20): el alias se puede repetir.
insert into jugador(id, alias, contrasena_hash, token_hash, creado_en, ultima_actividad) values
(1, 'ManoDeHierro', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-09-26 12:51-06', '2026-10-30 17:02-06'),
(2, 'la_tiburona', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-09-27 10:39-06', '2026-11-01 20:26-06'),
(3, 'PezGordo_MX', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-09-28 19:08-06', '2026-11-02 16:54-06'),
(4, 'Capitan_Anzuelo', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-09-29 19:19-06', '2026-10-20 16:37-06'),
(5, 'recien_llegado', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-11-03 12:08-06', null),
(6, 'Tiburon', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-09-30 12:04-06', '2026-10-19 11:31-06'),
(7, 'NeonCarpa', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-01 17:02-06', '2026-10-27 13:15-06'),
(8, 'doña_suerte', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-02 10:21-06', '2026-10-20 08:08-06'),
(9, 'ElBarracuda', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-02 10:25-06', '2026-10-28 17:35-06'),
(10, 'xX_Mojarra_Xx', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-04 12:17-06', '2026-10-28 17:44-06'),
(11, 'SirenaDelNorte', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-05 12:58-06', '2026-10-29 16:56-06'),
(12, 'RedDeOro', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-06 10:45-06', '2026-10-30 16:14-06'),
(13, 'BagreVeloz', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-08 12:19-06', '2026-10-31 09:50-06'),
(14, 'marea_alta', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-09 17:35-06', '2026-11-03 17:36-06'),
(15, 'Pulpo404', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-11 12:42-06', '2026-10-27 17:39-06'),
(16, 'CoralRojo', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-12 17:02-06', '2026-11-03 22:56-06'),
(17, 'Anguila_Zen', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-14 17:41-06', '2026-10-21 11:07-06'),
(18, 'TodoONada99', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-15 12:47-06', '2026-11-02 21:09-06'),
(19, 'Charal_Pro', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-17 10:27-06', '2026-10-25 18:33-06'),
(20, 'Tiburon', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-18 10:35-06', '2026-11-04 19:55-06'),
(21, 'ojo_de_pez', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-20 10:23-06', '2026-11-01 20:23-06'),
(22, 'Mantarraya', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-21 19:11-06', '2026-10-30 22:57-06'),
(23, 'PescadorNocturno', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-23 10:35-06', '2026-10-28 13:26-06'),
(24, 'Robalo_GDL', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-25 12:00-06', '2026-11-02 20:24-06'),
(25, 'GatoPescador', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-28 10:11-06', '2026-11-03 09:18-06'),
(26, 'FichaAzul', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-30 12:18-06', '2026-10-31 20:48-06'),
(27, 'BallenaFeliz', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-11-01 17:11-06', '2026-11-04 19:35-06'),
(28, 'Bucanero', '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku', encode(sha256(gen_random_uuid()::text::bytea), 'hex'), '2026-10-19 12:22-06', '2026-11-03 16:11-06');

-- Partidas de cada jugador en orden de fecha. ManoDeHierro (1) tiene 14: la PWA muestra 10 y "Ver más" las otras 4.
-- terminada_en: cuándo terminó la partida en el juego; creada_en: cuándo llegó a la API.
-- Las partidas 7 y 40 se jugaron sin conexión y llegaron horas después.
-- Las rondas pasan de 9 cuando el jugador siguió jugando después de la pantalla de victoria.
insert into partida(id, jugador_id, clave_idempotencia, terminada_en, resultado, ronda_alcanzada, puntaje, dinero_invertido, dinero_ganado, dinero_perdido, veces_todo_o_nada, empenos, prestamos, duracion_seg, creada_en) values
-- 1 ManoDeHierro
(1, 1, gen_random_uuid(), '2026-09-28 09:30-06', 'DERROTA', 6, 511, 5700, 4150, 3750, 0, 0, 0, 1802, '2026-09-28 09:30-06'),
(2, 1, gen_random_uuid(), '2026-09-30 14:34-06', 'ABANDONO', 2, 143, 1900, 1300, 800, 2, 0, 0, 453, '2026-09-30 14:34-06'),
(3, 1, gen_random_uuid(), '2026-10-03 08:19-06', 'DERROTA', 1, 60, 900, 600, 700, 0, 0, 0, 319, '2026-10-03 08:19-06'),
(4, 1, gen_random_uuid(), '2026-10-03 19:24-06', 'VICTORIA', 9, 1113, 9300, 10600, 1950, 0, 0, 1, 2606, '2026-10-03 19:24-06'),
(5, 1, gen_random_uuid(), '2026-10-07 16:55-06', 'DERROTA', 5, 436, 5050, 3800, 4400, 3, 0, 1, 1425, '2026-10-07 16:55-06'),
(6, 1, gen_random_uuid(), '2026-10-13 16:53-06', 'DERROTA', 3, 214, 2950, 1750, 2200, 0, 1, 0, 873, '2026-10-13 16:53-06'),
(7, 1, gen_random_uuid(), '2026-10-18 11:30-06', 'ABANDONO', 5, 435, 4950, 3550, 3400, 1, 0, 1, 1093, '2026-10-18 16:30-06'),
(8, 1, gen_random_uuid(), '2026-10-20 08:46-06', 'DERROTA', 3, 221, 2800, 2100, 1850, 1, 1, 1, 885, '2026-10-20 08:46-06'),
(9, 1, gen_random_uuid(), '2026-10-20 13:06-06', 'DERROTA', 2, 128, 1800, 1150, 1250, 0, 0, 0, 575, '2026-10-20 13:06-06'),
(10, 1, gen_random_uuid(), '2026-10-22 18:28-06', 'VICTORIA', 9, 1301, 8400, 9850, 1750, 0, 1, 0, 2576, '2026-10-22 18:28-06'),
(11, 1, gen_random_uuid(), '2026-10-25 20:30-06', 'DERROTA', 6, 570, 5350, 3350, 4400, 1, 0, 1, 1861, '2026-10-25 20:30-06'),
(12, 1, gen_random_uuid(), '2026-10-27 22:33-06', 'ABANDONO', 3, 210, 2850, 1600, 1750, 2, 0, 0, 671, '2026-10-27 22:33-06'),
(13, 1, gen_random_uuid(), '2026-10-30 08:45-06', 'DERROTA', 2, 148, 1800, 1400, 1450, 0, 0, 1, 581, '2026-10-30 08:45-06'),
(14, 1, gen_random_uuid(), '2026-10-30 17:02-06', 'VICTORIA', 9, 1200, 8100, 9900, 2050, 0, 0, 0, 2528, '2026-10-30 17:02-06'),
-- 2 la_tiburona
(15, 2, gen_random_uuid(), '2026-09-29 11:50-06', 'DERROTA', 2, 139, 1950, 1100, 1550, 2, 0, 0, 625, '2026-09-29 11:50-06'),
(16, 2, gen_random_uuid(), '2026-10-14 13:59-06', 'DERROTA', 6, 654, 5650, 4300, 4800, 0, 0, 0, 1747, '2026-10-14 13:59-06'),
(17, 2, gen_random_uuid(), '2026-10-14 20:46-06', 'DERROTA', 6, 602, 5750, 3850, 5050, 0, 1, 0, 1799, '2026-10-14 20:46-06'),
(18, 2, gen_random_uuid(), '2026-10-20 22:09-06', 'DERROTA', 2, 123, 1850, 1250, 1600, 0, 0, 0, 584, '2026-10-20 22:09-06'),
(19, 2, gen_random_uuid(), '2026-10-23 16:26-06', 'VICTORIA', 13, 2564, 11850, 13950, 2300, 1, 0, 0, 3697, '2026-10-23 16:26-06'),
(20, 2, gen_random_uuid(), '2026-10-27 20:08-06', 'ABANDONO', 6, 591, 5650, 3450, 3800, 0, 1, 1, 1420, '2026-10-27 20:08-06'),
(21, 2, gen_random_uuid(), '2026-10-30 20:32-06', 'VICTORIA', 9, 1355, 8750, 10050, 1550, 0, 1, 0, 2853, '2026-10-30 20:32-06'),
(22, 2, gen_random_uuid(), '2026-10-31 13:41-06', 'VICTORIA', 9, 1253, 8000, 9750, 2200, 0, 0, 0, 2777, '2026-10-31 13:41-06'),
(23, 2, gen_random_uuid(), '2026-11-01 20:26-06', 'VICTORIA', 9, 1386, 8650, 10750, 2050, 0, 0, 0, 2741, '2026-11-01 20:26-06'),
-- 3 PezGordo_MX
(24, 3, gen_random_uuid(), '2026-09-30 18:25-06', 'ABANDONO', 3, 235, 2900, 2150, 1300, 0, 0, 1, 593, '2026-09-30 18:25-06'),
(25, 3, gen_random_uuid(), '2026-09-30 22:31-06', 'VICTORIA', 9, 1358, 8600, 10200, 2400, 0, 0, 0, 2488, '2026-09-30 22:31-06'),
(26, 3, gen_random_uuid(), '2026-10-01 20:42-06', 'DERROTA', 5, 518, 4600, 3600, 3250, 2, 0, 0, 1458, '2026-10-01 20:42-06'),
(27, 3, gen_random_uuid(), '2026-10-05 11:25-06', 'DERROTA', 4, 347, 3950, 2800, 3400, 1, 0, 0, 1237, '2026-10-05 11:25-06'),
(28, 3, gen_random_uuid(), '2026-10-08 09:03-06', 'VICTORIA', 9, 1290, 8100, 9150, 1700, 1, 0, 1, 2584, '2026-10-08 09:03-06'),
(29, 3, gen_random_uuid(), '2026-10-20 18:00-06', 'DERROTA', 1, 52, 950, 500, 750, 2, 0, 0, 276, '2026-10-20 18:00-06'),
(30, 3, gen_random_uuid(), '2026-11-02 16:54-06', 'DERROTA', 4, 335, 3550, 2700, 2650, 0, 0, 0, 1042, '2026-11-02 16:54-06'),
-- 4 Capitan_Anzuelo
(31, 4, gen_random_uuid(), '2026-09-30 13:23-06', 'DERROTA', 6, 545, 5650, 4500, 5000, 1, 0, 0, 1738, '2026-09-30 13:23-06'),
(32, 4, gen_random_uuid(), '2026-10-04 13:04-06', 'DERROTA', 2, 131, 1900, 1100, 1250, 0, 0, 0, 573, '2026-10-04 13:04-06'),
(33, 4, gen_random_uuid(), '2026-10-11 19:10-06', 'DERROTA', 2, 128, 1800, 1200, 1250, 0, 0, 2, 589, '2026-10-11 19:10-06'),
(34, 4, gen_random_uuid(), '2026-10-14 14:19-06', 'DERROTA', 3, 198, 3050, 2200, 2750, 0, 0, 0, 917, '2026-10-14 14:19-06'),
(35, 4, gen_random_uuid(), '2026-10-18 16:45-06', 'VICTORIA', 9, 1215, 8650, 10500, 1700, 0, 1, 0, 2403, '2026-10-18 16:45-06'),
(36, 4, gen_random_uuid(), '2026-10-20 16:37-06', 'DERROTA', 2, 124, 2100, 1600, 1800, 0, 0, 0, 586, '2026-10-20 16:37-06'),
-- 6 Tiburon
(37, 6, gen_random_uuid(), '2026-10-01 16:50-06', 'DERROTA', 2, 134, 2000, 1150, 1550, 2, 0, 0, 556, '2026-10-01 16:50-06'),
(38, 6, gen_random_uuid(), '2026-10-14 14:54-06', 'ABANDONO', 3, 241, 3050, 1750, 1650, 0, 0, 0, 673, '2026-10-14 14:54-06'),
(39, 6, gen_random_uuid(), '2026-10-15 18:31-06', 'DERROTA', 3, 235, 3100, 2200, 2250, 0, 0, 0, 799, '2026-10-15 18:31-06'),
(40, 6, gen_random_uuid(), '2026-10-17 13:10-06', 'DERROTA', 4, 287, 4150, 2700, 3350, 2, 0, 0, 1139, '2026-10-18 07:10-06'),
(41, 6, gen_random_uuid(), '2026-10-19 11:31-06', 'VICTORIA', 9, 1151, 8750, 10650, 1700, 1, 0, 1, 2705, '2026-10-19 11:31-06'),
-- 7 NeonCarpa
(42, 7, gen_random_uuid(), '2026-10-03 11:49-06', 'DERROTA', 2, 130, 1850, 1450, 1500, 2, 0, 0, 558, '2026-10-03 11:49-06'),
(43, 7, gen_random_uuid(), '2026-10-13 14:07-06', 'VICTORIA', 9, 1301, 8700, 10000, 2100, 3, 1, 2, 2818, '2026-10-13 14:07-06'),
(44, 7, gen_random_uuid(), '2026-10-14 20:40-06', 'ABANDONO', 4, 323, 4100, 2250, 1800, 1, 1, 1, 824, '2026-10-14 20:40-06'),
(45, 7, gen_random_uuid(), '2026-10-24 08:49-06', 'DERROTA', 1, 61, 950, 650, 700, 0, 0, 0, 307, '2026-10-24 08:49-06'),
(46, 7, gen_random_uuid(), '2026-10-27 13:15-06', 'DERROTA', 7, 812, 6450, 3850, 5500, 1, 0, 0, 1915, '2026-10-27 13:15-06'),
-- 8 doña_suerte
(47, 8, gen_random_uuid(), '2026-10-10 08:54-06', 'VICTORIA', 9, 1206, 8450, 9600, 1700, 0, 1, 0, 2680, '2026-10-10 08:54-06'),
(48, 8, gen_random_uuid(), '2026-10-11 13:45-06', 'DERROTA', 3, 229, 2800, 1450, 2000, 0, 0, 1, 811, '2026-10-11 13:45-06'),
(49, 8, gen_random_uuid(), '2026-10-17 09:25-06', 'DERROTA', 6, 634, 5500, 4250, 5050, 0, 0, 2, 1861, '2026-10-17 09:25-06'),
(50, 8, gen_random_uuid(), '2026-10-20 08:08-06', 'ABANDONO', 4, 363, 3700, 2300, 1650, 0, 1, 0, 880, '2026-10-20 08:08-06'),
-- 9 ElBarracuda
(51, 9, gen_random_uuid(), '2026-10-09 19:02-06', 'VICTORIA', 11, 1769, 10150, 12100, 2550, 0, 0, 1, 3011, '2026-10-09 19:02-06'),
(52, 9, gen_random_uuid(), '2026-10-20 13:23-06', 'DERROTA', 3, 219, 3000, 1750, 2450, 0, 0, 0, 893, '2026-10-20 13:23-06'),
(53, 9, gen_random_uuid(), '2026-10-26 21:44-06', 'ABANDONO', 5, 455, 4700, 2400, 2200, 1, 0, 0, 1028, '2026-10-26 21:44-06'),
(54, 9, gen_random_uuid(), '2026-10-28 11:48-06', 'DERROTA', 1, 63, 900, 700, 700, 1, 0, 0, 283, '2026-10-28 11:48-06'),
(55, 9, gen_random_uuid(), '2026-10-28 14:23-06', 'DERROTA', 1, 61, 950, 550, 800, 1, 0, 0, 301, '2026-10-28 14:23-06'),
(56, 9, gen_random_uuid(), '2026-10-28 17:35-06', 'VICTORIA', 9, 1225, 8550, 9650, 2100, 2, 0, 0, 2530, '2026-10-28 17:35-06'),
-- 10 xX_Mojarra_Xx
(57, 10, gen_random_uuid(), '2026-10-06 09:20-06', 'ABANDONO', 4, 254, 3650, 1800, 1900, 0, 2, 0, 894, '2026-10-06 09:20-06'),
(58, 10, gen_random_uuid(), '2026-10-09 20:08-06', 'DERROTA', 1, 60, 900, 700, 650, 0, 0, 0, 265, '2026-10-09 20:08-06'),
(59, 10, gen_random_uuid(), '2026-10-28 17:44-06', 'DERROTA', 8, 862, 7800, 5050, 7150, 1, 0, 0, 2499, '2026-10-28 17:44-06'),
-- 11 SirenaDelNorte
(60, 11, gen_random_uuid(), '2026-10-15 14:53-06', 'VICTORIA', 9, 1133, 9050, 10450, 2000, 0, 0, 1, 2410, '2026-10-15 14:53-06'),
(61, 11, gen_random_uuid(), '2026-10-18 18:40-06', 'DERROTA', 4, 335, 3800, 2250, 3500, 0, 1, 1, 1069, '2026-10-18 18:40-06'),
(62, 11, gen_random_uuid(), '2026-10-29 13:26-06', 'DERROTA', 8, 1051, 7600, 4050, 6350, 2, 0, 0, 2320, '2026-10-29 13:26-06'),
(63, 11, gen_random_uuid(), '2026-10-29 16:56-06', 'DERROTA', 2, 122, 1850, 1050, 1550, 0, 0, 1, 629, '2026-10-29 16:56-06'),
-- 12 RedDeOro
(64, 12, gen_random_uuid(), '2026-10-25 19:32-06', 'VICTORIA', 9, 1275, 8850, 10350, 1850, 1, 0, 0, 2533, '2026-10-25 19:32-06'),
(65, 12, gen_random_uuid(), '2026-10-27 09:29-06', 'DERROTA', 4, 273, 3750, 2300, 2850, 2, 2, 0, 1259, '2026-10-27 09:29-06'),
(66, 12, gen_random_uuid(), '2026-10-27 21:33-06', 'DERROTA', 3, 211, 2950, 2350, 2550, 1, 0, 0, 835, '2026-10-27 21:33-06'),
(67, 12, gen_random_uuid(), '2026-10-30 16:14-06', 'ABANDONO', 2, 118, 2050, 1250, 1300, 1, 0, 1, 415, '2026-10-30 16:14-06'),
-- 13 BagreVeloz
(68, 13, gen_random_uuid(), '2026-10-08 14:53-06', 'DERROTA', 1, 50, 1050, 800, 850, 1, 0, 0, 316, '2026-10-08 14:53-06'),
(69, 13, gen_random_uuid(), '2026-10-09 14:47-06', 'DERROTA', 1, 56, 1000, 550, 700, 0, 0, 0, 276, '2026-10-09 14:47-06'),
(70, 13, gen_random_uuid(), '2026-10-31 09:50-06', 'DERROTA', 2, 130, 1800, 1050, 1500, 3, 0, 1, 521, '2026-10-31 09:50-06'),
-- 14 marea_alta
(71, 14, gen_random_uuid(), '2026-10-15 21:09-06', 'DERROTA', 2, 126, 1800, 1250, 1350, 0, 0, 0, 638, '2026-10-15 21:09-06'),
(72, 14, gen_random_uuid(), '2026-10-21 08:12-06', 'DERROTA', 4, 300, 4050, 2400, 3300, 1, 0, 1, 1068, '2026-10-21 08:12-06'),
(73, 14, gen_random_uuid(), '2026-10-24 13:34-06', 'DERROTA', 3, 216, 2650, 1450, 2400, 0, 1, 0, 816, '2026-10-24 13:34-06'),
(74, 14, gen_random_uuid(), '2026-10-24 21:42-06', 'ABANDONO', 3, 217, 3100, 2200, 1950, 0, 0, 0, 678, '2026-10-24 21:42-06'),
(75, 14, gen_random_uuid(), '2026-11-03 17:36-06', 'VICTORIA', 10, 1493, 9400, 11150, 2400, 3, 0, 0, 2781, '2026-11-03 17:36-06'),
-- 15 Pulpo404
(76, 15, gen_random_uuid(), '2026-10-20 18:49-06', 'DERROTA', 3, 212, 3000, 2000, 2250, 1, 0, 0, 913, '2026-10-20 18:49-06'),
(77, 15, gen_random_uuid(), '2026-10-26 09:20-06', 'DERROTA', 3, 198, 3100, 1900, 2400, 3, 0, 0, 938, '2026-10-26 09:20-06'),
(78, 15, gen_random_uuid(), '2026-10-27 17:39-06', 'ABANDONO', 1, 57, 1000, 550, 500, 1, 0, 0, 210, '2026-10-27 17:39-06'),
-- 16 CoralRojo
(79, 16, gen_random_uuid(), '2026-10-22 22:28-06', 'DERROTA', 4, 307, 4150, 3250, 3350, 0, 1, 0, 1120, '2026-10-22 22:28-06'),
(80, 16, gen_random_uuid(), '2026-10-26 20:17-06', 'DERROTA', 3, 204, 2900, 2300, 2450, 0, 0, 0, 822, '2026-10-26 20:17-06'),
(81, 16, gen_random_uuid(), '2026-11-03 22:56-06', 'VICTORIA', 9, 1230, 8000, 9600, 1850, 1, 0, 2, 2351, '2026-11-03 22:56-06'),
-- 17 Anguila_Zen
(82, 17, gen_random_uuid(), '2026-10-18 14:36-06', 'DERROTA', 4, 315, 3900, 2450, 2550, 0, 1, 0, 1269, '2026-10-18 14:36-06'),
(83, 17, gen_random_uuid(), '2026-10-21 11:07-06', 'ABANDONO', 3, 229, 3000, 1900, 1700, 1, 0, 0, 654, '2026-10-21 11:07-06'),
-- 18 TodoONada99
(84, 18, gen_random_uuid(), '2026-10-18 18:30-06', 'DERROTA', 8, 858, 7450, 5100, 6150, 6, 1, 1, 2099, '2026-10-18 18:30-06'),
(85, 18, gen_random_uuid(), '2026-10-23 08:55-06', 'VICTORIA', 9, 1237, 8500, 10000, 1450, 5, 0, 1, 2726, '2026-10-23 08:55-06'),
(86, 18, gen_random_uuid(), '2026-10-27 13:11-06', 'DERROTA', 6, 583, 5950, 3850, 4950, 4, 1, 1, 1567, '2026-10-27 13:11-06'),
(87, 18, gen_random_uuid(), '2026-11-02 21:09-06', 'DERROTA', 3, 248, 3050, 2100, 2750, 4, 0, 1, 785, '2026-11-02 21:09-06'),
-- 19 Charal_Pro
(88, 19, gen_random_uuid(), '2026-10-23 20:29-06', 'DERROTA', 2, 132, 1900, 1250, 1700, 1, 0, 0, 570, '2026-10-23 20:29-06'),
(89, 19, gen_random_uuid(), '2026-10-25 18:33-06', 'DERROTA', 8, 975, 8350, 5800, 7400, 1, 0, 0, 2210, '2026-10-25 18:33-06'),
-- 20 Tiburon
(90, 20, gen_random_uuid(), '2026-10-23 11:20-06', 'ABANDONO', 1, 52, 950, 500, 500, 0, 0, 0, 220, '2026-10-23 11:20-06'),
(91, 20, gen_random_uuid(), '2026-11-04 13:38-06', 'DERROTA', 1, 55, 900, 600, 750, 1, 0, 0, 261, '2026-11-04 13:38-06'),
(92, 20, gen_random_uuid(), '2026-11-04 19:55-06', 'DERROTA', 4, 325, 3550, 2750, 2900, 2, 0, 0, 1210, '2026-11-04 19:55-06'),
-- 21 ojo_de_pez
(93, 21, gen_random_uuid(), '2026-10-22 08:15-06', 'DERROTA', 4, 296, 3650, 2100, 2600, 2, 1, 2, 1173, '2026-10-22 08:15-06'),
(94, 21, gen_random_uuid(), '2026-11-01 20:23-06', 'DERROTA', 5, 428, 5100, 3200, 4050, 2, 0, 0, 1520, '2026-11-01 20:23-06'),
-- 22 Mantarraya
(95, 22, gen_random_uuid(), '2026-10-22 08:44-06', 'DERROTA', 6, 523, 5600, 4250, 4600, 0, 1, 0, 1767, '2026-10-22 08:44-06'),
(96, 22, gen_random_uuid(), '2026-10-27 14:34-06', 'DERROTA', 4, 326, 4100, 3250, 2900, 0, 1, 0, 1168, '2026-10-27 14:34-06'),
(97, 22, gen_random_uuid(), '2026-10-30 22:57-06', 'VICTORIA', 9, 1309, 8650, 10600, 1500, 2, 0, 0, 2708, '2026-10-30 22:57-06'),
-- 23 PescadorNocturno
(98, 23, gen_random_uuid(), '2026-10-24 16:53-06', 'ABANDONO', 1, 57, 1000, 800, 550, 2, 0, 0, 225, '2026-10-24 16:53-06'),
(99, 23, gen_random_uuid(), '2026-10-28 13:26-06', 'DERROTA', 8, 994, 7550, 5500, 6400, 2, 0, 1, 2525, '2026-10-28 13:26-06'),
-- 24 Robalo_GDL
(100, 24, gen_random_uuid(), '2026-11-01 16:45-06', 'DERROTA', 7, 772, 6800, 4400, 6150, 0, 0, 0, 2072, '2026-11-01 16:45-06'),
(101, 24, gen_random_uuid(), '2026-11-02 20:24-06', 'DERROTA', 4, 313, 4050, 2650, 3000, 1, 0, 1, 1187, '2026-11-02 20:24-06'),
-- 25 GatoPescador
(102, 25, gen_random_uuid(), '2026-11-03 09:18-06', 'DERROTA', 4, 355, 4150, 3150, 3300, 2, 0, 0, 1080, '2026-11-03 09:18-06'),
-- 26 FichaAzul
(103, 26, gen_random_uuid(), '2026-10-31 20:48-06', 'ABANDONO', 3, 203, 2800, 1550, 1650, 1, 1, 0, 696, '2026-10-31 20:48-06'),
-- 27 BallenaFeliz
(104, 27, gen_random_uuid(), '2026-11-04 19:35-06', 'VICTORIA', 9, 1152, 8350, 9400, 2000, 3, 0, 1, 2764, '2026-11-04 19:35-06'),
-- 28 Bucanero
(105, 28, gen_random_uuid(), '2026-10-30 11:27-06', 'DERROTA', 7, 792, 7000, 4850, 5150, 1, 0, 0, 1860, '2026-10-30 11:27-06'),
(106, 28, gen_random_uuid(), '2026-10-31 08:09-06', 'DERROTA', 7, 668, 6600, 4150, 4850, 2, 0, 2, 1988, '2026-10-31 08:09-06'),
(107, 28, gen_random_uuid(), '2026-11-03 16:11-06', 'DERROTA', 3, 226, 2850, 2250, 2000, 0, 0, 0, 893, '2026-11-03 16:11-06');

-- Comodines comprados y usados: 1 Multiplicador de puntos, 2 Bola adicional, 3 Bonificación económica, 4 Cambio de color
insert into partida_comodin(partida_id, comodin_id, veces_comprado, veces_usado) values
(4, 1, 2, 4),
(4, 3, 3, 3),
(4, 4, 1, 1),
(10, 1, 3, 9),
(10, 2, 3, 3),
(10, 3, 2, 2),
(14, 1, 1, 1),
(19, 1, 3, 3),
(21, 2, 2, 2),
(22, 1, 1, 3),
(23, 1, 2, 4),
(23, 2, 2, 2),
(25, 2, 2, 2),
(28, 3, 3, 3),
(35, 1, 2, 2),
(41, 1, 1, 2),
(41, 2, 2, 2),
(43, 3, 3, 3),
(46, 1, 1, 1),
(47, 1, 2, 6),
(47, 3, 3, 3),
(51, 1, 3, 9),
(56, 1, 3, 3),
(56, 2, 3, 3),
(59, 1, 1, 1),
(60, 1, 1, 1),
(62, 2, 3, 3),
(64, 1, 3, 3),
(64, 4, 1, 2),
(75, 2, 2, 2),
(75, 3, 1, 1),
(75, 4, 2, 4),
(81, 1, 1, 1),
(81, 3, 3, 3),
(81, 4, 1, 2),
(85, 2, 2, 2),
(86, 1, 3, 3),
(89, 4, 1, 3),
(95, 2, 1, 1),
(97, 1, 2, 6),
(97, 2, 1, 1),
(97, 4, 1, 1),
(99, 1, 2, 6),
(100, 2, 3, 3),
(104, 3, 1, 1);

-- Totales de cada jugador, calculados con la misma regla que la API (GameService.recalculate):
-- puntaje_maximo solo cuenta victorias; la racha cuenta derrotas seguidas y un abandono no la rompe.
-- Los jugadores sin victorias tienen puntaje_maximo_en null y no salen en el ranking.
-- NeonCarpa (7) y ManoDeHierro (1) empatan con 1301: el ranking pone primero al que lo logró antes.
insert into estadistica_jugador(jugador_id, puntaje_maximo, puntaje_maximo_en, partidas_totales, partidas_ganadas, dinero_invertido, dinero_ganado, dinero_perdido, racha_derrotas_max, tiempo_jugado_seg) values
(1, 1301, '2026-10-22 18:28-06', 14, 3, 61850, 55100, 31700, 4, 18248),
(2, 2564, '2026-10-23 16:26-06', 9, 4, 58100, 58450, 24900, 4, 18243),
(3, 1358, '2026-09-30 22:31-06', 7, 2, 32650, 31100, 15450, 2, 9678),
(4, 1215, '2026-10-18 16:45-06', 6, 1, 23150, 21100, 13750, 4, 6806),
(5, 0, null, 0, 0, 0, 0, 0, 0, 0),
(6, 1151, '2026-10-19 11:31-06', 5, 1, 21050, 18450, 10500, 3, 5872),
(7, 1301, '2026-10-13 14:07-06', 5, 1, 22050, 18200, 11600, 2, 6422),
(8, 1206, '2026-10-10 08:54-06', 4, 1, 20450, 17600, 10400, 2, 6232),
(9, 1769, '2026-10-09 19:02-06', 6, 2, 28250, 27150, 10800, 3, 8046),
(10, 0, null, 3, 0, 12350, 7550, 9700, 2, 3658),
(11, 1133, '2026-10-15 14:53-06', 4, 1, 22300, 17800, 13400, 3, 6428),
(12, 1275, '2026-10-25 19:32-06', 4, 1, 17600, 16250, 8550, 2, 5042),
(13, 0, null, 3, 0, 3850, 2400, 3050, 3, 1113),
(14, 1493, '2026-11-03 17:36-06', 5, 1, 21000, 18450, 11400, 3, 5981),
(15, 0, null, 3, 0, 7100, 4450, 5150, 2, 2061),
(16, 1230, '2026-11-03 22:56-06', 3, 1, 15050, 15150, 7650, 2, 4293),
(17, 0, null, 2, 0, 6900, 4350, 4250, 1, 1923),
(18, 1237, '2026-10-23 08:55-06', 4, 1, 24950, 21050, 15300, 2, 7177),
(19, 0, null, 2, 0, 10250, 7050, 9100, 2, 2780),
(20, 0, null, 3, 0, 5400, 3850, 4150, 2, 1691),
(21, 0, null, 2, 0, 8750, 5300, 6650, 2, 2693),
(22, 1309, '2026-10-30 22:57-06', 3, 1, 18350, 18100, 9000, 2, 5643),
(23, 0, null, 2, 0, 8550, 6300, 6950, 1, 2750),
(24, 0, null, 2, 0, 10850, 7050, 9150, 2, 3259),
(25, 0, null, 1, 0, 4150, 3150, 3300, 1, 1080),
(26, 0, null, 1, 0, 2800, 1550, 1650, 0, 696),
(27, 1152, '2026-11-04 19:35-06', 1, 1, 8350, 9400, 2000, 0, 2764),
(28, 0, null, 3, 0, 16450, 11250, 12000, 3, 4741);

-- gen_random_uuid() genera la clave que en la PWA crea el navegador
insert into retroalimentacion(id, clave_idempotencia, categoria, componente, mensaje, creada_en) values
(1, gen_random_uuid(), 'SUGERENCIA', 'PWA', 'Me gustaría filtrar mi historial por victorias y derrotas', '2026-10-12 11:05-06'),
(2, gen_random_uuid(), 'ERROR', 'JUEGO', 'La bola se quedó girando en la ronda 3 y tuve que reiniciar la partida', '2026-10-16 21:40-06'),
(3, gen_random_uuid(), 'SUGERENCIA', 'JUEGO', 'Estaría bien ver cuánto dinero llevo perdido antes de pedir un préstamo', '2026-10-21 18:20-06'),
(4, gen_random_uuid(), 'OTRO', 'PWA', 'Me ayudó ver en el expediente cuánto tiempo llevo jugando', '2026-10-26 13:15-06'),
(5, gen_random_uuid(), 'ERROR', 'PWA', 'En el celular el ranking tarda en cargar cuando tengo mala señal', '2026-10-30 08:50-06'),
(6, gen_random_uuid(), 'SUGERENCIA', 'OTRO', 'Podrían agregar una guía corta de cómo funcionan los comodines', '2026-11-02 19:30-06');

-- Encuesta: percepción del riesgo, cambio de opinión y claridad del mensaje (1 a 5); el comentario es opcional
insert into encuesta(id, clave_idempotencia, percepcion_riesgo, cambio_opinion, claridad_mensaje, comentario, enviada_en) values
(1, gen_random_uuid(), 4, 3, 5, 'No sabía que el préstamo hacía que se perdiera tan rápido', '2026-10-13 12:10-06'),
(2, gen_random_uuid(), 5, 4, 4, null, '2026-10-17 17:45-06'),
(3, gen_random_uuid(), 3, 2, 4, 'Me pareció un juego más que un mensaje, pero se entiende', '2026-10-20 20:05-06'),
(4, gen_random_uuid(), 5, 5, 5, 'El todo o nada da miedo después de ver el expediente', '2026-10-24 09:30-06'),
(5, gen_random_uuid(), 2, 2, 3, null, '2026-10-27 22:15-06'),
(6, gen_random_uuid(), 4, 4, 5, 'Me gustó que el dinero sea ficticio y aun así se sienta', '2026-10-31 16:40-06'),
(7, gen_random_uuid(), 4, 3, 4, null, '2026-11-02 11:25-06'),
(8, gen_random_uuid(), 5, 4, 4, 'Ver la racha de derrotas me hizo pensar', '2026-11-04 18:55-06');

-- Los id se insertaron a mano, así que cada secuencia se mueve al id más alto de su tabla:
-- el siguiente jugador que se registre será el 29 (o el siguiente al último jugador real).
select setval('jugador_id_seq', (select max(id) from jugador));
select setval('partida_id_seq', (select max(id) from partida));
select setval('retroalimentacion_id_seq', (select max(id) from retroalimentacion));
select setval('encuesta_id_seq', (select max(id) from encuesta));

commit;

-- Resumen
select count(*) as jugadores from jugador;
select count(*) as partidas from partida;
select j.id, j.alias, e.partidas_totales, e.partidas_ganadas, e.puntaje_maximo
from jugador j join estadistica_jugador e on e.jugador_id = j.id
where j.id between 1 and 28
order by j.id;
