# EducaShow — backend del sistema de gestión escolar

Backend REST monolítico modular para el sistema de gestión integral de un colegio. El alcance implementado incluye seguridad y cuentas, catálogo curricular, estudiantes, ventanas de matrícula, periodos académicos, matrículas, vacantes, plantillas horarias y registro de errores. El proyecto se desarrolla por historias de usuario; este README describe las funcionalidades que existen en el backend y no implica que todo el Product Backlog esté terminado.

## Guía de trabajo

El equipo integra cambios en `Development` mediante Pull Requests desde ramas personales permanentes; al cierre del sprint, integra `Development` en `main`. Antes de subir cambios, ejecuta `mvn spotless:apply`, `mvn spotless:check` y `mvn verify`. El flujo detallado está en [`docs/flujo-de-trabajo.md`](docs/flujo-de-trabajo.md).

## Tecnologías

- Java 25 (`pom.xml`), Spring Boot 3.5.6.
- Spring Web, Validation, Data JPA y Spring Security.
- MySQL para ejecución y pruebas locales.
- H2 para pruebas de integración.
- Flyway para migraciones y JJWT 0.13.0 para JWT.
- Maven y Spotless con Google Java Format.

## Funcionalidades y endpoints

Esta sección documenta las rutas REST implementadas. Para probarlas en Postman, consulta [`postman/README.md`](postman/README.md). `POST /api/v1/auth/login`, `POST /api/v1/auth/forgot-password` y `POST /api/v1/auth/reset-password` son públicas; las demás requieren autenticación Bearer.

### Convenciones generales

- Para las solicitudes con cuerpo, enviar `Content-Type: application/json`.
- Para las rutas protegidas, enviar `Authorization: Bearer <token>`.
- Las fechas se representan como `YYYY-MM-DD` (por ejemplo, `2026-03-01`).
- Los errores del API tienen el formato estándar `timestamp`, `status`, `error`, `message` y `path`. Se usan `400` para solicitudes inválidas, `401` para autenticación ausente o inválida, `403` para autorización denegada, `404` para recursos inexistentes, `409` para conflictos, `422` para algunas reglas de negocio y `500` para errores internos.
- Todas las rutas protegidas requieren autenticación. Las operaciones de cuentas, matrícula, estudiantes, períodos, vacantes, catálogo curricular, secciones, plantillas horarias y logs están restringidas al rol `ADMINISTRADOR` mediante autorización a nivel de controlador. `GET /api/v1/auth/me` está disponible para cualquier usuario autenticado; login y recuperación son públicos.
- La consulta y gestión de secciones está disponible solo para `ADMINISTRADOR`; el catálogo de grados se inicializa con los grados 1.º a 5.º de secundaria mediante Flyway.

#### Resumen de rutas

| Método | Endpoint | Propósito |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Iniciar sesión y obtener JWT |
| `GET` | `/api/v1/auth/me` | Consultar el perfil autenticado |
| `POST` | `/api/v1/auth/forgot-password` | Solicitar recuperación de contraseña |
| `POST` | `/api/v1/auth/reset-password` | Restablecer contraseña mediante token |
| `POST` | `/api/v1/cuentas-usuario` | Crear una cuenta (solo ADMINISTRADOR) |
| `GET` | `/api/v1/cuentas-usuario` | Listar cuentas (solo ADMINISTRADOR) |
| `PUT` | `/api/v1/cuentas-usuario/{id}` | Editar correo y datos personales de una cuenta (solo ADMINISTRADOR) |
| `PATCH` | `/api/v1/cuentas-usuario/{id}/estado` | Activar/desactivar una cuenta (solo ADMINISTRADOR) |
| `POST` | `/api/v1/cuentas-usuario/carga-masiva` | Cargar usuarios desde CSV (solo ADMINISTRADOR) |
| `GET` | `/api/v1/cuentas-usuario/carga-masiva/plantilla` | Descargar plantilla CSV (solo ADMINISTRADOR) |
| `GET` | `/api/v1/areas` | Listar áreas con competencias |
| `POST` | `/api/v1/areas` | Crear área con competencias iniciales |
| `GET` | `/api/v1/areas/{idOrCodigo}` | Consultar un área |
| `PUT` | `/api/v1/areas/{idOrCodigo}` | Actualizar un área |
| `PATCH` | `/api/v1/areas/{id}/estado` | Activar/desactivar un área |
| `POST` | `/api/v1/areas/{areaId}/competencias` | Agregar competencia al área |
| `DELETE` | `/api/v1/areas/{idOrCodigo}` | Desactivar un área |
| `POST` | `/api/v1/competencias` | Crear una competencia |
| `GET` | `/api/v1/competencias/{idOrCodigo}` | Consultar una competencia |
| `PUT` | `/api/v1/competencias/{idOrCodigo}` | Actualizar una competencia |
| `PATCH` | `/api/v1/competencias/{id}/estado` | Activar/desactivar competencia |
| `DELETE` | `/api/v1/competencias/{idOrCodigo}` | Desactivar una competencia |
| `POST` | `/api/v1/estudiantes` | Crear una cuenta y perfil de estudiante |
| `GET` | `/api/v1/estudiantes` | Listar estudiantes con filtros y paginación |
| `POST` | `/api/v1/estudiantes/{cuentaUsuarioId}/asignar` | Asignar perfil de estudiante a una cuenta existente |
| `GET` | `/api/v1/secciones` | Listar secciones y resumen de vacantes (solo ADMINISTRADOR) |
| `POST` | `/api/v1/secciones` | Crear sección (solo ADMINISTRADOR) |
| `PATCH` | `/api/v1/secciones/{id}` | Actualizar capacidad de sección (solo ADMINISTRADOR) |
| `GET` | `/api/v1/horarios/plantillas` | Listar plantillas horarias (solo ADMINISTRADOR) |
| `POST` | `/api/v1/horarios/plantillas` | Crear plantilla de bloques (solo ADMINISTRADOR) |
| `GET` | `/api/v1/horarios/plantillas/{id}` | Consultar plantilla (solo ADMINISTRADOR) |
| `PUT` | `/api/v1/horarios/plantillas/{id}` | Actualizar plantilla (solo ADMINISTRADOR) |
| `PATCH` | `/api/v1/horarios/plantillas/{id}/estado` | Activar/desactivar plantilla (solo ADMINISTRADOR) |
| `DELETE` | `/api/v1/horarios/plantillas/{id}` | Desactivar plantilla (solo ADMINISTRADOR) |
| `POST` | `/api/v1/periodos` | Crear un periodo académico |
| `POST` | `/api/v1/periodos-matricula` | Configurar ventana de matrícula |
| `GET` | `/api/v1/periodos-matricula` | Listar ventanas y estado calculado |
| `GET` | `/api/v1/periodos-matricula/{id}` | Consultar ventana y estado calculado |
| `PUT` | `/api/v1/periodos-matricula/{id}` | Actualizar ventana de matrícula |
| `DELETE` | `/api/v1/periodos-matricula/{id}` | Desactivar ventana de matrícula |
| `GET` | `/api/v1/periodos/{periodoId}/vacantes` | Consultar vacantes de un periodo |
| `POST` | `/api/v1/periodos/{periodoId}/matriculas` | Registrar una matrícula |
| `PATCH` | `/api/v1/periodos/{periodoId}/matriculas/{estudianteId}/confirmar` | Confirmar una matrícula en registro |
| `DELETE` | `/api/v1/periodos/{periodoId}/matriculas/{estudianteId}` | Eliminar una matrícula |
| `GET` | `/api/v1/logs/errores` | Consultar logs paginados (solo ADMINISTRADOR) |
| `GET` | `/api/v1/logs/errores/{id}` | Consultar el detalle de un log (solo ADMINISTRADOR) |

### Secciones y vacantes (US-1.7)

Las tres rutas de secciones requieren rol `ADMINISTRADOR`. `GET /api/v1/secciones` devuelve una lista ordenada por grado y nombre de sección:

```json
[
  {
    "id": 12,
    "gradoNumero": 1,
    "nombre": "A",
    "vacantesTotales": 30,
    "cantidadMatriculados": 2,
    "vacantesDisponibles": 28
  }
]
```

`cantidadMatriculados` se calcula desde las matrículas: cuenta estudiantes distintos con matrícula activa en estado `EN_REGISTRO` o `CONFIRMADA` asociados a la sección. Las matrículas retiradas, inactivas o finalizadas no cuentan.

`vacantesDisponibles` es la capacidad total menos `cantidadMatriculados`, con mínimo cero. Si una sección antigua tiene capacidad nula o no positiva, el listado la representa como `0` y `0` vacantes disponibles para que una fila inválida no interrumpa el resto del listado.

`POST /api/v1/secciones` crea una sección para un grado de secundaria entre 1 y 5. El nombre debe contener una o dos letras mayúsculas; la capacidad debe ser un entero positivo.

```json
{
  "gradoNumero": 1,
  "nombre": "A",
  "vacantesTotales": 30
}
```

Devuelve `201` con la sección creada. Si ya existe una sección con ese nombre en el grado, devuelve `409`.

`PATCH /api/v1/secciones/{id}` actualiza solo la capacidad y recibe `{ "vacantesTotales": 32 }`. Devuelve `200` con el resumen actualizado. Si el valor no es positivo, devuelve `400`; si es menor que la cantidad de alumnos matriculados, devuelve `422`; si la sección no existe o está inactiva, devuelve `404`.

### Plantillas horarias (US-4.3)

Las rutas `/api/v1/horarios/plantillas` requieren rol `ADMINISTRADOR`. Una plantilla define turno (`MANANA` o `TARDE`), hora de inicio y fin, duración de hora pedagógica y cantidad de bloques pedagógicos. El backend genera los bloques en secuencia e inserta recreos configurados después de un bloque determinado. La hora final calculada no puede exceder el fin del turno; se permite un margen libre al final.

`POST /api/v1/horarios/plantillas` recibe, por ejemplo:

```json
{
  "nombre": "Turno mañana",
  "turno": "MANANA",
  "horaInicio": "07:30:00",
  "horaFin": "13:00:00",
  "duracionHoraPedagogicaMinutos": 45,
  "cantidadBloquesPedagogicos": 6,
  "recreos": [
    { "despuesDeBloque": 2, "duracionMinutos": 15 },
    { "despuesDeBloque": 4, "duracionMinutos": 20 }
  ]
}
```

`recreos` puede ser un arreglo vacío. No se permiten dos recreos tras el mismo bloque ni ubicaciones fuera de la cantidad configurada. La respuesta contiene la configuración y los bloques con orden, tipo (`PEDAGOGICO`/`RECREO`), hora de inicio y fin. `GET` lista plantillas activas e inactivas; `PUT` reemplaza su configuración y regenera los bloques; `PATCH /{id}/estado` cambia su disponibilidad; `DELETE` la desactiva sin borrarla físicamente. Los nombres deben ser únicos dentro de cada turno.

### Autenticación

#### `POST /api/v1/auth/login`

Público. Recibe `username` y `password`, ambos obligatorios y no vacíos. Valida que la cuenta esté activa y no bloqueada y compara la contraseña con BCrypt. Un login correcto reinicia los intentos fallidos, registra auditoría y devuelve un token.

```json
{
  "username": "usuario@ejemplo.edu.pe",
  "password": "contraseña"
}
```

Respuesta `200`:

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "usuario": {
    "id": 12,
    "username": "usuario@ejemplo.edu.pe",
    "rol": "ADMINISTRADOR",
    "nombreCompleto": "Nombre Apellido",
    "dni": "12345678"
  }
}
```

Contraseña incorrecta, cuenta inexistente, inactiva o bloqueada producen `401` con el mismo mensaje: `Credenciales inválidas`. Para reducir diferencias de tiempo que podrían revelar si el usuario existe, el backend también ejecuta una verificación BCrypt ficticia al rechazar cuentas inexistentes, inactivas o bloqueadas. Los datos de login vacíos producen `400`.

#### `GET /api/v1/auth/me`

No recibe parámetros ni cuerpo. Requiere JWT y responde `200` con el objeto `usuario` mostrado arriba (`id`, `username`, `rol`, `nombreCompleto`, `dni`).

#### Recuperación de contraseña

`POST /api/v1/auth/forgot-password` es público y recibe `{ "username": "usuario@ejemplo.edu.pe" }`. `username` puede ser un nombre de usuario que no tenga formato de correo; el mensaje se envía al correo registrado en `Persona` cuando está disponible. Responde con un mensaje genérico tanto si la cuenta existe como si no, para no revelar cuentas registradas. Si corresponde, envía por correo un token aleatorio; en la base se almacena su hash y tiene expiración configurable mediante `PASSWORD_RESET_EXPIRATION_MINUTES` (15 minutos por defecto). El administrador semilla local usa un correo de prueba no supervisado; para completar la recuperación se necesita configurar un destinatario accesible.

`POST /api/v1/auth/reset-password` es público y recibe `{ "token": "<token-recibido-por-correo>", "password": "nueva-clave-segura" }`. El token es de un solo uso y la contraseña se almacena con BCrypt.

### Cuentas de usuario

`POST /api/v1/cuentas-usuario`, `GET /api/v1/cuentas-usuario`, `PUT /api/v1/cuentas-usuario/{id}` y `PATCH /api/v1/cuentas-usuario/{id}/estado` están restringidos al rol `ADMINISTRADOR`. El alta recibe DNI, nombre, apellidos, correo (`username`), contraseña y uno de los roles `ADMINISTRADOR`, `DOCENTE`, `ALUMNO` o `APODERADO`. El username debe tener formato de correo y hasta 50 caracteres; la contraseña debe tener entre 8 y 72 caracteres. Al crear una cuenta ALUMNO o APODERADO también crea su perfil correspondiente. La contraseña se almacena con BCrypt. La consulta devuelve las cuentas con datos personales separados y estado activo. La actualización modifica nombre, apellidos y correo; valida que el correo no pertenezca a otra cuenta y conserva el DNI, el rol y la contraseña. El cambio de estado recibe `{ "activo": true|false }`, devuelve la cuenta actualizada y registra el cambio en auditoría. No se permite al administrador desactivar su propia cuenta. Las operaciones de escritura se registran en auditoría. La carga CSV corresponde a US-5.9.

El login y el filtro JWT rechazan las cuentas inactivas, por lo que la desactivación surte efecto también para tokens emitidos previamente.

#### Carga masiva de usuarios (US-5.9)

`GET /api/v1/cuentas-usuario/carga-masiva/plantilla` descarga un ejemplo. `POST /api/v1/cuentas-usuario/carga-masiva` recibe una parte `multipart/form-data` llamada `archivo` con extensión `.csv`. Ambas rutas requieren rol `ADMINISTRADOR`. La cabecera requiere `rol`, `dni`, `nombres`, `apellido_paterno`, `apellido_materno` y `correo`; admite `telefono`, `fecha_nacimiento`, `dni_apoderado` y `parentesco`. El archivo admite comas o punto y coma y hasta 500 filas. Los roles permitidos son `DOCENTE`, `ALUMNO` y `APODERADO`; los alumnos requieren DNI de apoderado y parentesco.

La carga acepta CSV UTF-8 y Windows-1252 (formato común al exportar desde Excel en Windows), de hasta 10 MB por archivo. Antes de guardar en Excel, importa la columna `dni` como texto para evitar que se eliminen ceros iniciales; una vez eliminados al exportar, el backend no puede reconstruirlos. El DNI se conserva como texto y admite letras para documentos alternativos. La respuesta de carga usa `Cache-Control: no-store` porque incluye contraseñas temporales de cuentas nuevas. Los archivos que superan el límite se responden con `413` y mensaje estándar.

Cada fila se valida y procesa de forma independiente. Las filas válidas se guardan aunque otras sean rechazadas. Un DNI existente actualiza los datos personales permitidos sin reemplazar la cuenta, la contraseña, el rol ni las matrículas; un correo duplicado de otra cuenta o un cambio de rol se rechaza. La carga sincroniza el correo con `Persona`, y para alumnos crea o actualiza el vínculo familiar existente. Los campos opcionales vacíos conservan el valor previo de una persona existente. Las personas sin cuenta reciben una nueva cuenta activa y una contraseña temporal que solo se devuelve en el resultado de esa carga.

La respuesta contiene `totalFilas`, `usuariosCreados`, `usuariosActualizados`, `usuariosVinculados`, `usuariosSinCambios`, `filasRechazadas`, `cuentas` (incluye contraseñas temporales solo para cuentas nuevas), `resultados` y `errores` con número de fila, campo y motivo. Responde `201` si todas las filas se procesaron y se crearon cuentas; `200` si hubo procesamiento parcial o solo actualizaciones/vínculos; `422` si ninguna fila pudo procesarse o el archivo no tiene estructura válida. Los cambios de cada fila se confirman en una transacción independiente.

Ejemplo de `PUT /api/v1/cuentas-usuario/12`:

```json
{
  "nombre": "Ana",
  "apellidoPaterno": "Pérez",
  "apellidoMaterno": "Díaz",
  "username": "ana.perez@colegio.edu.pe"
}
```

### Áreas y competencias (US-3.5)

El catálogo permite crear, editar y desactivar áreas y competencias. El listado expone competencias anidadas con su estado. Las rutas aceptan IDs numéricos y códigos legados donde corresponde. Las escrituras están restringidas a `ADMINISTRADOR`.

#### Áreas

`GET /api/v1/areas` lista el catálogo. `POST /api/v1/areas` crea un área (puede incluir competencias anidadas). `GET` consulta por ID o código, `PUT` actualiza datos y relaciones, `PATCH /estado` cambia el estado y `DELETE` desactiva lógicamente (`204`), sin borrar físicamente.

El cuerpo de creación y actualización tiene estos campos:

| Campo | Tipo | Requisito |
| --- | --- | --- |
| `codigo` | string | Obligatorio, no vacío, máximo 50 caracteres; debe coincidir con `{codigo}` en `PUT` |
| `nombre` | string | Obligatorio, no vacío, máximo 150 caracteres |
| `descripcion` | string | Opcional, máximo 500 caracteres |
| `competenciaCodigos` | arreglo de strings | Opcional; en actualización reemplaza relaciones si se incluye |
| `competencias` | arreglo de objetos | Opcional en creación; competencias anidadas con nombre y descripción |

Ejemplo:

```json
{
  "codigo": "MAT",
  "nombre": "Matemática",
  "descripcion": "Área curricular de matemática",
  "competenciaCodigos": ["MAT-01", "MAT-02"]
}
```

Al actualizar, omitir `competenciaCodigos` conserva las relaciones; enviarlo vacío las quita. El alta admite competencias anidadas. La desactivación lógica conserva las relaciones.

#### Competencias

`POST` crea una competencia; `GET` consulta por ID o código; `PUT` actualiza datos y relaciones; `PATCH /estado` cambia estado; `DELETE` desactiva lógicamente (`204`).

El cuerpo de creación y actualización es equivalente al de área, con `areaCodigos` en lugar de `competenciaCodigos`:

```json
{
  "codigo": "MAT-01",
  "nombre": "Resuelve problemas de cantidad",
  "descripcion": "Descripción opcional",
  "areaCodigos": ["MAT"]
}
```

Al actualizar, omitir `areaCodigos` conserva las relaciones; enviarlo vacío las quita. Las competencias se desactivan de forma lógica. La validación de notas registradas queda pendiente hasta que exista el módulo de notas.

### Estudiantes

#### `POST /api/v1/estudiantes`

Crea una Persona, un perfil de estudiante y una cuenta nueva con rol `ALUMNO`. No se recibe el rol en el cuerpo; la contraseña se almacena con BCrypt. Devuelve `201` con:

```json
{
  "estudianteId": 31,
  "personaId": 45,
  "username": "alumno01",
  "rol": "ALUMNO",
  "dni": "12345678",
  "nombreCompleto": "Ana Pérez Rojas"
}
```

Campos esperados:

| Campo | Tipo | Requisito |
| --- | --- | --- |
| `username` | string | Obligatorio, no vacío, máximo 50 caracteres |
| `password` | string | Obligatorio, de 8 a 72 caracteres |
| `dni` | string | Obligatorio, no vacío, máximo 15 caracteres |
| `nombre` | string | Obligatorio, no vacío, máximo 100 caracteres |
| `apellidoPaterno` | string | Obligatorio, no vacío, máximo 100 caracteres |
| `apellidoMaterno` | string | Obligatorio, no vacío, máximo 100 caracteres |
| `fechaNacimiento` | fecha | Opcional, formato `YYYY-MM-DD` |
| `telefono` | string | Opcional, máximo 20 caracteres |
| `genero` | string | Opcional, máximo 20 caracteres |
| `correo` | string | Opcional, debe tener formato de correo, máximo 254 caracteres |

DNI o username ya registrados producen `409`.

#### `GET /api/v1/estudiantes`

Lista estudiantes activos y sus matrículas en una página. Acepta filtros opcionales `nombre` (búsqueda parcial en nombres y apellidos), `dni` (búsqueda parcial), `gradoId` y `seccionId`; también acepta `page` (desde 0, predeterminado 0) y `size` (predeterminado 20, máximo 100). Las matrículas se consultan aparte de la página de estudiantes para evitar paginar una colección cargada en memoria.

#### `POST /api/v1/estudiantes/{cuentaUsuarioId}/asignar`

`cuentaUsuarioId` es el ID entero de una cuenta existente. No recibe cuerpo. Asigna el rol `ALUMNO` y crea el perfil de estudiante asociado a la Persona de la cuenta; responde `200` con la misma estructura de `EstudianteResponse`. La cuenta debe existir y estar vinculada a una Persona. Si no existe, responde `404`; si no tiene Persona o ya tiene perfil de estudiante, responde `400` o `409`.

### Ventana de matrícula (US-1.1)

La ventana de matrícula es global y separada del periodo académico asociado a grado y sección. Todas las rutas requieren rol `ADMINISTRADOR`. El sistema considera ambas fechas inclusivas: antes de `fechaInicio` el estado es `PENDIENTE`, entre ambas fechas es `VIGENTE` y después de `fechaLimite` es `VENCIDO`. La fecha límite debe ser posterior a la fecha de inicio.

`POST /api/v1/periodos-matricula` recibe `{ "fechaInicio": "2026-10-20", "fechaLimite": "2026-10-31" }` y responde `201` con `id`, las fechas y `estado`. `GET` lista las ventanas activas con su estado actual y `GET /{id}` consulta una. `PUT /{id}` actualiza ambas fechas. `DELETE /{id}` la desactiva lógicamente para que deje de habilitar matrículas. Las fechas inválidas responden `400`; una ventana inexistente o ya desactivada responde `404`.

El endpoint de registro `POST /api/v1/periodos/{periodoId}/matriculas` solo acepta altas mientras exista una ventana global en estado `VIGENTE`; fuera de ella responde `409`. Esta validación no afecta la confirmación ni el retiro de matrículas ya registradas.

### Periodos académicos y vacantes

#### `POST /api/v1/periodos`

Crea un periodo para un grado y sección existentes. Recibe:

```json
{
  "gradoId": 1,
  "seccionId": 2,
  "fechaInicio": "2026-03-01",
  "fechaFin": "2026-12-20"
}
```

Los cuatro campos son obligatorios. `gradoId` y `seccionId` son enteros; las fechas son `YYYY-MM-DD` y `fechaInicio` debe ser anterior o igual a `fechaFin`. El grado y la sección deben existir y pertenecer entre sí. El estado se establece como `PLANIFICADO`; no se envía en la solicitud.

Responde `201` con `{ "id", "gradoId", "seccionId", "fechaInicio", "fechaFin", "estado" }`. Un grado o sección inexistente responde `404`; fechas inconsistentes o sección de otro grado, `400`; periodo duplicado, `409`.

#### `GET /api/v1/periodos/{periodoId}/vacantes`

`periodoId` es un entero de ruta. No recibe query parameters ni cuerpo. Responde `200` con:

```json
{
  "periodoId": 7,
  "gradoId": 1,
  "numeroGrado": 1,
  "nivelEducativo": "PRIMARIA",
  "seccionId": 2,
  "seccion": "A",
  "fechaInicio": "2026-03-01",
  "fechaFin": "2026-12-20",
  "vacantesTotales": 30,
  "matriculasActivas": 24,
  "vacantesDisponibles": 6,
  "hayVacantes": true
}
```

`matriculasActivas` cuenta estudiantes distintos con matrícula activa en la sección y estado `EN_REGISTRO` o `CONFIRMADA` (pueden pertenecer a distintos periodos de esa sección). La vacante se reserva al crear la matrícula en `EN_REGISTRO`; confirmar no consume otra. `vacantesDisponibles` es el total de la sección menos ese conteo, con mínimo cero. `hayVacantes` indica si el resultado es mayor a cero. Periodo inexistente responde `404`; capacidad de sección ausente o no válida responde `400`.

### Matrículas

#### `POST /api/v1/periodos/{periodoId}/matriculas`

Registra a un estudiante en el periodo indicado. `periodoId` es un entero de ruta. El cuerpo requiere `estudianteId` entero:

```json
{
  "estudianteId": 31
}
```

Si hay cupo y no existe una matrícula activa duplicada, crea la matrícula o reactiva el registro retirado/inactivo. Devuelve `201` con:

```json
{
  "matriculaId": 55,
  "estudianteId": 31,
  "periodoId": 7,
  "fechaMatricula": "2026-02-20",
  "estado": "EN_REGISTRO"
}
```

Periodo o estudiante inexistente responde `404`; falta de vacantes o matrícula activa duplicada responde `409`. Una capacidad no configurada o inválida en la sección responde `400`. El cupo queda reservado desde el estado `EN_REGISTRO`.

#### `PATCH /api/v1/periodos/{periodoId}/matriculas/{estudianteId}/confirmar`

Confirma la matrícula activa del estudiante en el periodo. No recibe cuerpo y devuelve `200` con la matrícula en estado `CONFIRMADA`. Solo se puede confirmar desde `EN_REGISTRO`; una segunda confirmación o un estado distinto responde `409`. Si no existe la matrícula activa, responde `404`.

#### `DELETE /api/v1/periodos/{periodoId}/matriculas/{estudianteId}`

`periodoId` y `estudianteId` son enteros de ruta. No recibe cuerpo y responde `204` sin contenido al eliminar la matrícula. Si no existe el periodo o la relación de matrícula, responde `404`.

### Logs de errores

`GET /api/v1/logs/errores` y `GET /api/v1/logs/errores/{id}` están restringidos a `ADMINISTRADOR`. La lista admite filtros opcionales `nivel`, `modulo`, `desde` y `hasta`, fechas ISO `YYYY-MM-DD` y paginación Spring (`page`, `size`, `sort`). El tamaño predeterminado es 20 y el máximo es 100. La lista omite la traza; el detalle la incluye cuando fue capturada. Las trazas se truncan a 4000 caracteres. Los errores internos 500 conservan su traza en el log, pero no la devuelven al cliente.

## Estructura

```text
src/main/java/pe/edu/pucp/colegio/
├── ColegioBackendApplication.java
├── common/
│   ├── audit/       # Auditoría y persistencia de errores
│   ├── controller/  # Catálogos, estudiantes, periodos, matrículas, ventanas y logs
│   ├── dto/         # Solicitudes y respuestas
│   ├── exception/   # Excepciones de negocio y manejo HTTP global
│   ├── model/       # Personas, catálogo curricular, organización y matrículas
│   ├── repository/  # Persistencia de los módulos comunes
│   └── service/     # Lógica de negocio de los módulos comunes
└── security/
    ├── config/      # Reglas de acceso y cadena de filtros
    ├── controller/  # Autenticación y gestión de cuentas
    ├── dto/         # Solicitudes y respuestas
    ├── jwt/         # Emisión, validación y filtro JWT
    ├── model/       # CuentaUsuario y Rol
    ├── repository/  # Acceso a cuentas
    └── service/     # Autenticación, cuentas y recuperación de contraseña

src/main/resources/
├── application.yml
└── db/migration/V*__*.sql

src/test/
├── java/.../*IntegrationTest.java
└── resources/application.yml
```

Flyway crea las tablas de seguridad, catálogo curricular, ventanas globales de matrícula, periodos académicos, matrículas y organización académica. Hibernate valida el esquema al iniciar (`ddl-auto: validate`). La ventana de matrícula controla cuándo se permiten nuevas altas; un `Periodo` académico pertenece a un grado y sección y puede contener varias `Matricula`. Cada matrícula enlaza un estudiante con un solo periodo y un estudiante puede matricularse en distintos periodos. Un `Grado` se identifica por número y nivel educativo (`INICIAL`, `PRIMARIA` o `SECUNDARIA`); así se pueden tener, por ejemplo, los grados 1 de Primaria y 1 de Secundaria. Cada grado requiere al menos una `Seccion`, identificada por un código como `A` o `B`, y cada sección pertenece a un solo grado.

## Requisitos

- JDK 25, requerido para compilar y ejecutar el proyecto.
- Maven.
- Un servidor MySQL accesible en `localhost:3306`.

Este checkout todavía no contiene Maven Wrapper ni los workflows de CI descritos en el diseño técnico del repositorio; por ahora se requiere una instalación local de Maven y ejecutar las verificaciones manualmente.

Para instalar los requisitos y levantar MySQL, backend y frontend en tu máquina, sigue [`docs/despliegue-local.md`](docs/despliegue-local.md). El flujo de ramas y Pull Requests está en [`docs/flujo-de-trabajo.md`](docs/flujo-de-trabajo.md).

### Instalar Maven
```bash
sudo dnf install maven
mvn -version
```

En Windows, instala Apache Maven y agrega su carpeta `bin` al `PATH`, o usa Maven desde tu IDE.

## Instalación de la base de datos

La aplicación no instala el servidor de base de datos. El servicio MySQL debe estar activo antes de arrancar el backend.

### Fedora con MySQL

Instala MySQL Server desde los repositorios de Fedora y activa el servicio:

```bash
sudo dnf install mysql-server
sudo systemctl enable --now mysqld
sudo systemctl status mysqld
```

Si en una instalación limpia el servicio falla indicando que `/var/lib/mysql` no existe, crea el directorio con el propietario y permisos esperados y vuelve a iniciar MySQL:

```bash
sudo install -d -o mysql -g mysql -m 750 /var/lib/mysql
sudo restorecon -Rv /var/lib/mysql
sudo systemctl start mysqld
```

Configura la cuenta administrativa de MySQL:

```bash
sudo mysql_secure_installation
```

Luego inicia sesión como `root` y crea una base y un usuario exclusivo para la aplicación. Cambia `TU_CONTRASEÑA` por una contraseña propia:

```bash
sudo mysql -u root
```

```sql
CREATE DATABASE colegio_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'educashow'@'localhost' IDENTIFIED BY 'PASSWORD_2026';
GRANT ALL PRIVILEGES ON colegio_db.* TO 'educashow'@'localhost';
EXIT;
```

### Windows con MySQL

1. Descarga MySQL Community Server desde [MySQL Downloads](https://dev.mysql.com/downloads/).
2. Ejecuta el instalador MSI y completa la configuración con MySQL Configurator. Configura MySQL como servicio y asigna una contraseña al usuario administrativo `root`.
3. Abre MySQL Command Line Client desde el menú Inicio e inicia sesión como `root`, o usa PowerShell con `mysql -u root -p` si el cliente está en `PATH`.
4. Ejecuta estas instrucciones SQL, cambiando `PASSWORD_2026`:

```sql
CREATE DATABASE colegio_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'educashow'@'localhost' IDENTIFIED BY 'PASSWORD_2026';
GRANT ALL PRIVILEGES ON colegio_db.* TO 'educashow'@'localhost';
```

El instalador MSI con MySQL Configurator es el método recomendado por MySQL para Windows. MySQL 8.4 puede requerir Microsoft Visual C++ 2019 Redistributable.

## Configuración del backend

Define las variables en la misma terminal desde la que ejecutarás Spring Boot. `JWT_SECRET` es obligatorio; si falta, la aplicación falla al crear `JwtTokenProvider` antes de intentar conectarse a la base de datos.

### Con archivo `.env` (recomendado en local)

Copia `.env.example` a `.env` en la raíz del repositorio y completa los valores (`JWT_SECRET` se genera con `openssl rand -base64 32`). Spring Boot lo lee al arrancar mediante `spring.config.import`; las variables exportadas en la terminal tienen prioridad sobre el archivo. `.env` está en `.gitignore` y nunca debe versionarse. Los pasos completos están en [`docs/despliegue-local.md`](docs/despliegue-local.md).

### Fedora (Bash)

```bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/colegio_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USERNAME='educashow'
export DB_PASSWORD='PASSWORD_2026'
export JWT_SECRET="$(openssl rand -base64 32)"
mvn spring-boot:run
```

### Windows (PowerShell)

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/colegio_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:DB_USERNAME = 'educashow'
$env:DB_PASSWORD = 'PASSWORD_2026'
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
mvn spring-boot:run
```

En Fedora y Windows se usa MySQL Connector/J con el prefijo `jdbc:mysql:`.

### Variables disponibles

| Variable | Uso | Valor predeterminado |
|---|---|---|
| `PORT` | Puerto HTTP | `8080` |
| `DB_URL` | URL JDBC de la base | MySQL local `colegio_db` |
| `DB_USERNAME` | Usuario de base de datos | `root` |
| `DB_PASSWORD` | Contraseña de base de datos | `root` |
| `JWT_SECRET` | Clave Base64 para firmar JWT | Obligatoria |
| `JWT_EXPIRATION_MS` | Duración del token | `86400000` ms (24 horas) |
| `MAX_LOGIN_ATTEMPTS` | Intentos antes del bloqueo | `3` |
| `LOCK_TIME_MINUTES` | Duración del bloqueo | `15` minutos |
| `LOG_FILE` | Archivo de logs | `logs/colegio-backend.log` |
| `MAIL_USERNAME` | Cuenta SMTP para la recuperación de contraseña | Vacío |
| `MAIL_PASSWORD` | Contraseña SMTP (en Gmail, una contraseña de aplicación) | Vacío |

## Verificación manual

Con la base de datos activa y las variables configuradas, inicia la aplicación con `mvn spring-boot:run`. Flyway aplica las migraciones disponibles y Hibernate valida las entidades contra las tablas. Si el inicio falla, revisa el primer `Caused by` en el log: puede indicar una variable faltante, un error de conexión o un esquema incompatible.

La migración inicial crea una cuenta administradora semilla para desarrollo local. No uses esa cuenta en entornos compartidos o desplegados; debe rotarse o deshabilitarse antes de cualquier despliegue. En una base donde V1 ya fue aplicada, modificar el archivo V1 no actualiza los datos existentes; la rotación debe hacerse mediante una actualización separada.

Para probar el login en un entorno local recién inicializado:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin@educore.edu.pe","password":"<contrasenia-local>"}'
```

Usa el valor `token` de la respuesta:

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H 'Authorization: Bearer <token>'
```

## Pruebas

El proyecto cuenta con pruebas unitarias y de integración para autenticación y recuperación, cuentas, catálogo, estudiantes, ventanas y periodos de matrícula, vacantes, plantillas horarias, autorización y registro de errores. Las pruebas de integración usan Spring Boot, MockMvc y H2; no sustituyen una verificación final contra MySQL.

```bash
mvn test
```

## Avance del Sprint 1 y pendientes

| Historia | Avance del backend |
| --- | --- |
| US-1.1 Periodo de matrícula | Implementada: ventana global independiente, estados calculados por fecha y bloqueo de nuevas matrículas fuera de vigencia. |
| US-1.2 Registrar alumnos y asignar grado/sección | Implementada en backend: alta de estudiantes, listado con filtros y paginación, asignación de perfil a cuentas y matrícula por periodo académico con confirmación y retiro lógico. |
| US-1.7 Vacantes por grado/sección | Implementada: capacidad, resumen y control de vacantes; no permite reducir capacidad por debajo de matrículas activas (`422`). |
| US-3.5 Áreas y competencias | CRUD y desactivación lógica implementados. La protección basada en notas queda pendiente hasta implementar notas. |
| US-4.3 Bloques y franjas horarias | Implementada en backend: plantillas por turno con bloques pedagógicos y recreos calculados, validación del rango horario y administración de estado. La integración con interfaz de usuario queda pendiente. |
| US-5.1 Activar/desactivar cuentas (extra) | Implementada: cambio de estado de cuentas; cuentas inactivas no pueden iniciar sesión ni usar tokens existentes. |
| US-5.2 Roles y permisos | Implementada para las rutas existentes: roles estrictos `ADMINISTRADOR`, `DOCENTE`, `ALUMNO` y `APODERADO`; las funciones administrativas de los módulos están protegidas para `ADMINISTRADOR`. Cuentas permiten alta, consulta y edición acotada de correo y datos personales. |
| US-5.5 Log de errores | Implementada: captura, persistencia y consulta administrativa con filtros, paginación y detalle. |
| US-5.6 Login y recuperar contraseña | Implementada en backend con BCrypt, bloqueo y token temporal. La entrega por correo requiere un destinatario/servicio accesible. |
| US-5.9 Carga masiva CSV | Implementada en backend: carga CSV protegida para administrador, procesamiento parcial por fila, actualización de personas/cuentas existentes sin cambiar contraseña o rol, creación de perfiles/vínculos y reporte de resultados. La interfaz de usuario aún está pendiente. |

El estado actual de las historias se describe en la tabla anterior. Las instrucciones de la colección de Postman están en [`postman/README.md`](postman/README.md).

La arquitectura objetivo se describe en el Documento de Arquitectura del proyecto: aplicación Spring Boot monolítica modular, MySQL con migraciones Flyway, frontend SPA independiente y despliegue planeado en AWS. El despliegue en AWS no está configurado en este repositorio.

## Referencias

- [Fedora Project: MySQL Server](https://fedoraproject.org/wiki/Changes/MySQL_9.7)
- [MySQL 8.4: instalación en Windows](https://dev.mysql.com/doc/refman/8.4/en/windows-installation.html)
- [MySQL 8.4: crear una base de datos](https://dev.mysql.com/doc/refman/8.4/en/create-database.html)
