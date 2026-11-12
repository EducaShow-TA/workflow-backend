# Colecciones Postman de EducaShow

Esta carpeta contiene dos colecciones con objetivos distintos:

| Archivo | Uso |
| --- | --- |
| `EducaShow-Sprint1.postman_collection.json` | Ejecutar solicitudes encadenadas con aserciones para comprobar flujos de API. |
| `EducaShow-Sprint1.postman_environment.json` | URL, credenciales locales y variables usadas por la colección de comprobación. |
| `EducaShow-Carga-Manual-Sprint1-y-2.postman_collection.json` | Crear un escenario de datos para que una persona explore manualmente las historias desde Postman o la interfaz. No contiene aserciones de resultado. |
| `EducaShow-Carga-Manual-Local.postman_environment.json` | Environment local separado para la carga manual. |

Para el estado funcional de backend e interfaz por historia, consulta [el README principal](../README.md#estado-actual-de-historias--sprint-1-y-sprint-2). La colección de carga prepara datos de ejemplo; no prueba por sí sola todos los criterios de aceptación.

## Requisitos

- Backend disponible en `http://localhost:8080` y esquema local actualizado mediante Flyway.
- Base de datos local de desarrollo o pruebas. Ambas colecciones escriben datos; no las ejecutes contra una base que contenga información que quieras conservar.
- El administrador semilla local disponible. En una instalación nueva sus credenciales son `admin01` / `Admin2026!`; si se cambió la contraseña, actualiza el environment correspondiente.

## Colección de comprobación automatizada

Importa `EducaShow-Sprint1.postman_collection.json` y `EducaShow-Sprint1.postman_environment.json`, y selecciona **EducaShow - Local**. Ejecuta la colección completa en orden con el Runner. El login guarda el JWT y las solicitudes encadenadas almacenan los IDs de estudiantes, matrículas, compromisos, boletas, comprobantes y otros recursos.

Los flujos incluyen autenticación y recuperación, catálogo curricular, ventanas y matrículas, finanzas, cuentas y roles, carga CSV, logs, plantillas horarias, tareas y evidencias. La colección crea datos persistentes. Al final retira las matrículas y desactiva las ventanas de matrícula creadas, pero conserva estudiantes, cuentas, catálogos, periodos, compromisos, boletas, comprobantes, secciones y otros registros. Cada ejecución puede acumular datos.

La colección presupone que `gradoId=1` es el primer grado de secundaria sembrado por Flyway. Si se usa una base con catálogo personalizado, confirma ese ID antes de ejecutarla. La plantilla CSV generada por la colección usa datos aleatorios y no requiere seleccionar un archivo local.

### Recuperación de contraseña y archivos de evidencia

La colección comprueba la respuesta genérica de `Solicitar recuperación`; no se espera recibir correo con la configuración local predeterminada. Para completar el restablecimiento se necesita configurar correo saliente, acceder al mensaje y copiar el token real en `resetToken`.

Las solicitudes de carga/descarga de archivos de evidencia se omiten por defecto en el Runner. Para probarlas, habilita `runFileEvidencePostman=true`, adjunta un archivo de hasta 10 MB en `Registrar calificación con archivo` y luego ejecuta `Descargar archivo adjunto`. Para probar el límite, habilita `runOversizeEvidencePostman=true` y adjunta un archivo mayor a 10 MB en `Rechazar archivo de evidencia mayor a 10 MB`; se espera `413`. Restablece ambas variables a `false` al terminar. La evidencia mediante URL sí forma parte del flujo automatizado.

## Collection de carga manual

Importa `EducaShow-Carga-Manual-Sprint1-y-2.postman_collection.json` y `EducaShow-Carga-Manual-Local.postman_environment.json`, selecciona **EducaShow - Carga manual local** y ejecuta las solicitudes en orden. El login genera un identificador nuevo por ejecución y guarda automáticamente los IDs de recursos creados.

El escenario contiene dos estudiantes matriculados y confirmados con apoderados y compromisos de pago; también crea un docente, un apoderado y otro estudiante mediante CSV. Añade un área con competencias, periodo académico, sección, aula, plantilla y horario semanal; crea una sesión de clase con una asistencia, una evaluación parcial, una calificación con evidencia URL y una tarea con entregas asociadas.

Los dos alumnos creados directamente usan la contraseña `Alumno2026!`; sus usernames son `alu<seedTag>1` y `alu<seedTag>2`. La carga CSV devuelve contraseñas temporales para las cuentas nuevas; guarda la respuesta si vas a iniciar sesión como docente, apoderado o el tercer alumno. Las fechas del periodo, la ventana, los vencimientos y la tarea se calculan al ejecutar la colección.

La carga manual también deja datos persistentes y no tiene solicitudes de limpieza. Elige automáticamente una sección libre de primer grado. Si ya existe una ventana global vigente, omite **Crear ventana de matrícula vigente** y usa la existente. Para volver a cargar datos, ejecuta de nuevo desde el login: la ejecución genera nuevos usernames, documentos, área y aula.

Esta carga facilita explorar matrícula, vacantes, apoderados, pagos, roles, catálogo, asistencia, notas, libretas, tareas, evidencias y horarios. Para recuperación por correo, evidencia como archivo, justificación documental, auditoría, cierre de actas, mora y escenarios negativos, usa las solicitudes automatizadas o crea los datos específicos que requiera cada caso.

## Límites conocidos del backend

- **US-1.6 Morosidad:** el proceso diario actualiza las boletas vencidas impagas. No hay una ruta para adelantar el reloj ni una comprobación Postman determinista; el proceso corre a la hora configurada en el servidor.
- **US-1.12 Pago en línea:** la API registra un pago y genera comprobante, pero no procesa pagos mediante un proveedor externo.
- **Recuperación de contraseña:** el flujo completo depende de un servicio SMTP y de un buzón accesible.
- **Riesgo académico (US-3.4):** se calcula en la interfaz docente según las notas `C`; el backend no expone un endpoint específico para esta detección.

Los pagos del módulo de pensiones requieren rol `ADMINISTRADOR`. Para contratos de rutas, formatos de respuesta y limitaciones adicionales, consulta [el README principal](../README.md).
