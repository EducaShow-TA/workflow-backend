# Pruebas de API con Postman

Esta carpeta contiene la colección de solicitudes encadenadas para revisar APIs del Sprint 1 y las historias de pensiones del Sprint 2:

- `EducaShow-Sprint1.postman_collection.json`: solicitudes y validaciones automatizadas de matrícula, pensiones y otros módulos.
- `EducaShow-Sprint1.postman_environment.json`: URL local, credenciales de desarrollo y variables que la colección completa durante la ejecución.

## Requisitos y preparación

1. Inicia el backend en `http://localhost:8080` y verifica que Flyway haya aplicado todas las migraciones incluidas en esta rama.
2. Importa ambos JSON en Postman y selecciona el environment **EducaShow - Local**.
3. Ejecuta la colección completa en orden con **Run**. La solicitud de login obtiene el JWT y la colección guarda los IDs que generan las solicitudes posteriores, incluidos `matriculaId`, `compromisoId`, `boletaId` y `comprobanteId`; no es necesario rellenarlos manualmente.

Se recomienda ejecutar la colección contra una **base de datos local de pruebas recién creada** (esquema aplicado desde cero por Flyway y datos semilla). La colección presupone que `gradoId=1` existe y corresponde al primer grado de secundaria sembrado por la migración V12. En una base existente o personalizada, confirma ese ID y actualiza `gradoId` en el environment.

No restaures ni borres una base que contenga información que quieras conservar. Si necesitas repetir el flujo desde limpio, usa una base aislada de desarrollo/pruebas y vuelve a crearla desde Flyway.

## Credenciales y correo de recuperación

La colección usa el administrador semilla local:

| Variable | Valor inicial |
| --- | --- |
| `adminUsername` | `admin01` |
| `adminEmail` | `admin@educore.edu.pe` |
| `adminPassword` | `Admin2026!` |

Son credenciales exclusivamente de desarrollo. Si la contraseña fue cambiada en esa base, actualiza `adminPassword` en el environment.

La dirección del administrador es un correo de prueba, no un buzón real supervisado. `Solicitar recuperación` envía el campo `correo` y valida la respuesta genérica, pero **no se espera recibir un correo ni completar el restablecimiento** en la configuración local predeterminada. La solicitud `Restablecer contraseña` se omite automáticamente mientras `resetToken` conserve el valor de marcador. Solo reemplázalo por un token real cuando hayas configurado correo saliente y puedas leer el mensaje recibido.

El campo `adminUsername` se usa para probar el inicio de sesión con username; el endpoint también acepta el correo de contacto como identificador. El campo de recuperación usa siempre `adminEmail`.

## Qué ejecuta la colección

Las carpetas se deben ejecutar en el orden presentado:

1. **Autenticación**: inicia sesión como administrador y guarda el Bearer token; solicita recuperación de contraseña. El restablecimiento queda omitido sin token real.
2. **Catálogo curricular (US-3.5)**: crea y consulta una competencia independiente, crea un área con competencias, edita el área, añade/edita una competencia, activa/desactiva recursos y comprueba las rutas `DELETE` de área y competencia.
3. **Ventana y matrícula (US-1.1 / US-1.2 / US-1.7)**: configura y comprueba estados pendiente, vigente y vencido, rechaza fechas inválidas, filtra estudiantes por nombre y DNI con paginación, registra dos estudiantes, confirma una matrícula con los términos financieros, rechaza duplicados y retira las matrículas. Comprueba también el bloqueo fuera de vigencia y que reducir capacidad por debajo de la ocupación resulte en `422`.
4. **Pensiones y cobranza (US-1.3 / US-1.4 / US-1.5 / US-1.10 / US-1.12)**: consulta el compromiso y la primera boleta creados por la confirmación, genera una boleta manual, rechaza su duplicado, rechaza un pago por monto incorrecto, registra el monto exacto, verifica el estado `PAGADA`, consulta el comprobante y el reporte, y rechaza un rango de vencimiento invertido.
5. **Cuentas y roles (US-5.1 / US-5.2 / US-5.9)**: crea una cuenta, edita nombre/apellidos/correo, comprueba que desactivarla impida login y uso del token previo, la reactiva y asigna un perfil de estudiante. También verifica el rechazo de acceso anónimo y de un usuario DOCENTE. La petición de US-5.9 genera y carga automáticamente un CSV con APODERADO, ALUMNO vinculado y DOCENTE; usa DNI y correos aleatorios para permitir ejecuciones repetidas.
6. **Registro de errores**: provoca/consulta los errores previstos y verifica el listado y el detalle de logs.
7. **Franjas horarias y plantillas**: crea una plantilla de turno mañana, valida bloques y recreos calculados, consulta y actualiza la plantilla, cambia su estado y finalmente la desactiva. La colección guarda el ID para encadenar las solicitudes; valida que ocho bloques de 45 minutos no quepan en el turno mañana de 08:00 a 13:00 y que la API responda `422` por exceso de duración.

Antes del bloque de matrícula académica, la colección configura una ventana global vigente de US-1.1 con inicio ayer y límite a 30 días, además de crear ventanas futuras y vencidas para comprobar sus estados. La configuración se genera en cada ejecución, por lo que el alta de matrículas puede comprobarse sin editar fechas a mano. Al final vence temporalmente la ventana principal, comprueba que el registro responda `409` y desactiva lógicamente las tres ventanas que creó. Así no quedan ventanas activas que habiliten matrículas en ejecuciones futuras. La API de ventana permite listar, consultar, actualizar y desactivar periodos.

La colección calcula el primer vencimiento como el día 10 del siguiente mes, de modo que los casos financieros no dependan de una fecha fija. La segunda boleta manual se genera para el mes posterior al primero. La confirmación y las pruebas de pensiones usan al estudiante 1 y su matrícula; por eso ejecuta la colección completa y en orden.

La colección crea datos de prueba. Elimina las matrículas y desactiva las ventanas de matrícula creadas durante el flujo; conserva compromisos, boletas, comprobantes, cuentas, estudiantes, áreas, competencias, periodos académicos, secciones y plantillas generados (la plantilla creada por US-4.3 queda desactivada al final). Cada ejecución puede añadir registros. Para resultados previsibles y evitar acumular datos, ejecútala sobre una base aislada de pruebas; la creación de secciones busca nombres libres y una base con muchas secciones puede agotar las combinaciones disponibles.

## Resultados y límites conocidos

- Un error de Postman como `Detalle de log (incluye stackTrace)` puede depender del tipo de registro: la traza técnica solo está presente cuando el backend capturó una excepción, especialmente para errores internos `500`; no todos los errores `4xx` tienen `stackTrace`. Para verificar el criterio de que un `500` se guarda con traza sin exponerla al cliente, consulta la prueba de integración `RegistroError500IntegrationTest`.
- La recuperación por correo no es una prueba de extremo a extremo hasta configurar un SMTP y un destinatario real/de prueba accesible.
- US-1.6 no tiene una prueba Postman determinista: la mora se actualiza por el proceso diario (01:10, hora local del servidor) y la API no expone una ruta para adelantar el reloj. Para comprobarla en una base aislada, genera una boleta pendiente, cambia temporalmente su `fecha_vencimiento` a ayer en la base de pruebas, espera al siguiente ciclo diario y consulta `GET /api/v1/boletas-pension/{id}`; debe devolver `estadoPago=EN_MORA`. No hagas esta modificación en una base con datos reales.
- US-1.12 en esta entrega prueba el registro administrativo de un pago marcado `VIRTUAL` y su comprobante. No prueba procesamiento con una pasarela externa; esa integración queda fuera del backend actual y pendiente de definición del equipo. Todas las rutas de pensiones documentadas aquí requieren rol `ADMINISTRADOR`.
- La colección incluye la descarga de plantilla y una carga CSV automatizada de US-5.9. El script genera un CSV multipart con APODERADO antes de ALUMNO y una fila DOCENTE; cada fila incluye usernames y correos de contacto separados y únicos. Las aserciones comprueban las tres altas, el vínculo familiar y las contraseñas temporales. El Runner no requiere seleccionar un archivo local. US-1.1 cubre estados, fechas inválidas y bloqueo fuera de vigencia; US-1.2 cubre el listado paginado y la asignación de un perfil. US-4.3 está cubierta mediante el grupo de franjas horarias y plantillas.
- Al editar CSV con Excel, importa la columna `dni` como texto antes de guardar para conservar ceros iniciales. La carga admite UTF-8 y Windows-1252.
- La edición de cuenta se hace con `PUT /api/v1/cuentas-usuario/{id}` y modifica nombre, apellidos, `username` y correo de contacto como campos separados. La colección verifica que el correo no reemplace el username; la actualización conserva DNI, rol y contraseña.
- Las rutas de cuentas, estudiantes, períodos/ventanas/matrículas, áreas/competencias, secciones, plantillas horarias y logs requieren el rol `ADMINISTRADOR`. La colección comprueba los casos anónimo y DOCENTE sobre cuentas, el estado de cuentas inactivas y la asignación de perfil de estudiante; las pruebas de integración cubren también el rechazo de DOCENTE en las rutas administrativas.
- US-1.7 utiliza secciones, periodos académicos y matrículas existentes. El modelo de periodo académico ligado a grado/sección no equivale por sí mismo a la ventana de matrícula descrita en US-1.1.

Para el estado del backend por historia y el contrato de las rutas, consulta [`../README.md`](../README.md).
