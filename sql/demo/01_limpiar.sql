-- Fat Fish: borra los datos de demostración (FF-129)
-- Solo borra los id que carga 02_cargar.sql: jugador 1 a 28, retroalimentacion 1 a 6 y encuesta 1 a 8
-- (con las partidas y estadísticas de esos jugadores). Los jugadores reales tienen id mayores y no se tocan.
-- Se puede correr las veces que se quiera; para volver a cargar, después corre 02_cargar.sql.

begin;

-- En el orden de las llaves foráneas; partida_comodin se borra en cascada con cada partida
delete from partida where jugador_id between 1 and 28;
delete from estadistica_jugador where jugador_id between 1 and 28;
delete from jugador where id between 1 and 28;
delete from retroalimentacion where id between 1 and 6;
delete from encuesta where id between 1 and 8;

commit;

select count(*) as jugadores from jugador;
select count(*) as partidas from partida;
select count(*) as retroalimentaciones from retroalimentacion;
select count(*) as encuestas from encuesta;
