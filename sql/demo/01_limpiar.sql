-- Fat Fish: borra los datos de demostración (FF-129)
-- Solo borra filas de 02_cargar.sql. Un id en el rango no basta: también tiene que coincidir una marca
-- de la demostración, para no borrar datos reales que hayan tomado esos id:
--   jugador 1 a 28 con el hash fijo de su contraseña (BCrypt usa una sal aleatoria, así que ningún
--   jugador real tiene ese mismo hash), con sus partidas y estadísticas;
--   retroalimentacion 1 a 6 y encuesta 1 a 8 con la fecha exacta en que las carga 02_cargar.sql.
-- Se puede correr las veces que se quiera; para volver a cargar, después corre 02_cargar.sql.
-- Si un jugador real ocupa uno de esos id, 02_cargar.sql falla por id repetido sin cambiar nada.

begin;

-- En el orden de las llaves foráneas; partida_comodin se borra en cascada con cada partida
delete from partida
where jugador_id in (select id from jugador where id between 1 and 28
                     and contrasena_hash = '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku');

delete from estadistica_jugador
where jugador_id in (select id from jugador where id between 1 and 28
                     and contrasena_hash = '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku');

delete from jugador
where id between 1 and 28
  and contrasena_hash = '$2a$10$bxMmxvlFCJ1TNv7umiZefuCYrDsYF7Xs0lS/VCUcIB5DJUo9X7hku';

delete from retroalimentacion
where (id, creada_en) in (
  (1, '2026-10-12 11:05-06'),
  (2, '2026-10-16 21:40-06'),
  (3, '2026-10-21 18:20-06'),
  (4, '2026-10-26 13:15-06'),
  (5, '2026-10-30 08:50-06'),
  (6, '2026-11-02 19:30-06'));

delete from encuesta
where (id, enviada_en) in (
  (1, '2026-10-13 12:10-06'),
  (2, '2026-10-17 17:45-06'),
  (3, '2026-10-20 20:05-06'),
  (4, '2026-10-24 09:30-06'),
  (5, '2026-10-27 22:15-06'),
  (6, '2026-10-31 16:40-06'),
  (7, '2026-11-02 11:25-06'),
  (8, '2026-11-04 18:55-06'));

commit;

select count(*) as jugadores from jugador;
select count(*) as partidas from partida;
select count(*) as retroalimentaciones from retroalimentacion;
select count(*) as encuestas from encuesta;
