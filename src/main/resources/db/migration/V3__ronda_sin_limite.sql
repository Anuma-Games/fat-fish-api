alter table partida drop constraint partida_ronda_alcanzada_check;
alter table partida add constraint partida_ronda_alcanzada_check check (ronda_alcanzada >= 1);