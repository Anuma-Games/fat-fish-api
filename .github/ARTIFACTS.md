# Descargas y conservación de artefactos

## Última versión descargable

[Descargar la última compilación de main](https://github.com/Anuma-Games/fat-fish-api/releases/latest).

Después de pasar todos los jobs de CI, un push a `main` publica `fat-fish-api.jar` en Releases.
El JAR es el mismo que pasó las comprobaciones de CI.
No caduca por antigüedad: permanece aunque no haya cambios durante meses o falle el siguiente CI.
`develop` y los pull requests no reemplazan esta descarga.

Primero se suben los archivos a un borrador y se publica; solo después se eliminan las releases automáticas
anteriores y sus tags `build-main-<run_id>-<intento>`. Las releases manuales quedan intactas.
Un reintento de un commit anterior no sustituye el build de un commit más reciente.
El tag apunta al commit compilado; las notas identifican el commit y la ejecución.
El job de publicación necesita `contents: write` solo en `main`. No usa PAT ni modifica el código.
Si se activan releases inmutables o reglas que prohíban borrar estos tags, habrá que adaptar la limpieza;
no se desactivan esas protecciones automáticamente.

## Copias temporales de Actions

- La limpieza corre al terminar todo CI, diariamente a las 07:37 UTC y de forma manual.
- Conserva un build del último CI exitoso que no provenga de un pull request, entre todas las ramas.
  Si todavía no hay uno exitoso, conserva el último build de una rama.
- Conserva los dos reportes más recientes de cada tipo, aunque las pruebas hayan fallado.
- Respeta las ejecuciones activas: un JAR no se borra mientras otro job pueda necesitarlo.
  Los builds de pull requests se eliminan después de terminar CI y sus consumidores.
- Revisa de nuevo el estado de cada ejecución justo antes del borrado, para respetar reintentos.
- No toca artefactos con nombres ajenos a la política, releases, cachés, Packages ni archivos del repositorio.
- Las copias temporales de Actions caducan a los 7 días (builds) o 3 días (reportes), incluso si la limpieza falla.
  Esto no afecta la descarga permanente de Releases.

El workflow necesita llegar a la rama predeterminada para activarse. Desde Actions → Limpiar artefactos,
una ejecución manual muestra el plan; hay que marcar «aplicar» para borrar. Sin `--apply`, el script tampoco borra.
Usa `GITHUB_TOKEN` del repositorio, sin PAT ni permisos en otros repositorios. En `workflow_run` solo ejecuta
el script de la rama predeterminada, nunca código de un PR.

La cuota de la organización se comparte entre repositorios. La política reduce la acumulación, pero no
revierte el consumo ya acumulado ni garantiza espacio para builds simultáneos de cualquier tamaño.
Las pruebas siguen siendo obligatorias; liberar almacenamiento no altera sus resultados.

La descarga de Releases empezará a existir tras integrar estos workflows y completar el primer CI exitoso de `main`.
Los uploads temporales siguen sujetos a la cuota de Actions: este cambio no elimina un bloqueo ya acumulado del ciclo actual.

El job «Verificar conservación de builds» ejecuta las pruebas de los scripts también en los pull requests.
