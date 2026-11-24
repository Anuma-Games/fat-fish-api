-- El juego registra cada comodín por separado. Los ids 1 a 4 pasan a ser los comodines
-- que representaban a su familia; los demás comodines del juego se agregan con ids nuevos.
update comodin set nombre = 'Zona caliente', descripcion = 'Tu bono de la casa vale x1.5 (+0.5 por nivel)' where id = 1;
update comodin set nombre = 'Tridente', descripcion = 'Lanza una bola más por nivel y cuenta la que más te paga; la casa se queda con 20% de tu ganancia por cada bola extra' where id = 2;
update comodin set nombre = 'Boleto dorado', descripcion = 'Ganas 10% más dinero en cada giro ganador (+5% por nivel), con un límite por ronda' where id = 3;
update comodin set nombre = 'Dado rojo', descripcion = 'Pinta de rojo 3 casillas a tu elección (+2 por nivel)' where id = 4;

insert into comodin(id, nombre, descripcion) values
(5, 'Dado oscuro', 'Pinta de negro 3 casillas a tu elección (+2 por nivel)'),
(6, 'Marea verde', 'Pinta de verde 4 casillas a tu elección (+4 por nivel)'),
(7, 'Gemelos', 'Reemplaza bolsillos de la ruleta por un número elegido'),
(8, 'Caos de la fortuna', 'Cada ronda cambia al azar el color de 6 casillas (+2 por nivel)'),
(9, 'Espejo mágico', 'Una vez por ronda copia un bolsillo sobre otro de la ruleta (+1 por nivel)'),
(10, 'Reloj de arena', '+1 giro en cada ronda por nivel'),
(11, 'Ficha cargada', 'Los plenos suman +2 de multiplicador al bono de la casa por nivel'),
(12, 'Seguro de la casa', 'Te regresa 10% de lo que pierdes en cada giro (+10% por nivel)'),
(13, 'Abanico de fichas', 'Cada jugada secundaria que gana suma +0.08 al combo del bono por nivel');
