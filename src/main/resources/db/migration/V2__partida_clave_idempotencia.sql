-- Fat Fish: cada partida se identifica con la clave (UUID) que genera el juego, no con un contador local.
-- Con el contador, dos dispositivos de la misma cuenta podían enviar el mismo número y una partida se perdía.
-- terminada_en es la fecha en que el juego terminó la partida (para ordenar el historial y la racha).

alter table partida drop column numero_local;

-- Las partidas que ya existan reciben una clave nueva y su fecha de registro como fecha de término
alter table partida add column clave_idempotencia uuid not null unique default gen_random_uuid();
alter table partida alter column clave_idempotencia drop default;

alter table partida add column terminada_en timestamptz;
update partida set terminada_en = creada_en;
alter table partida alter column terminada_en set not null;

drop index idx_partida_jugador;
create index idx_partida_jugador on partida(jugador_id, terminada_en desc);
