# EducaShow — backend del sistema de gestión escolar

Backend REST monolítico modular para el sistema de gestión integral de un colegio. El alcance implementado incluye seguridad y cuentas, catálogo curricular, estudiantes, periodos académicos, matrículas, vacantes y registro de errores. El proyecto se desarrolla por historias de usuario; este README describe las funcionalidades que existen en el backend y no implica que todo el Product Backlog esté terminado.

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

Esta sección documenta las rutas REST implementadas. Hay 25 operaciones. `POST /api/v1/auth/login`, `POST /api/v1/auth/forgot-password` y `POST /api/v1/auth/reset-password` son públicas; las demás requieren autenticación Bearer.

### Convenciones generales

- Para las solicitudes con cuerpo, enviar `Content-Type: application/json`.
- Para las rutas protegidas, enviar `Authorization: Bearer <token>`.
- Las fechas se representan como `YYYY-MM-DD` (por ejemplo, `2026-03-01`).
- Los errores del API tienen el formato estándar `timestamp`, `status`, `error`, `message` y `path`. Se usan `400` para solicitudes inválidas, `401` para autenticación ausente o inválida, `403` para autorización denegada, `404` para recursos inexistentes, `409` para conflictos, `422` para algunas reglas de negocio y `500` para errores internos.
- Todas las rutas protegidas requieren autenticación. La gestión de cuentas de usuario y la consulta de logs de errores están restringidas al rol `ADMINISTRADOR`. La autorización por rol para el resto de las funcionalidades aún debe completarse según US-5.2.
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
| `POST` | `/api/v1/areas` | Crear un área |
| `GET` | `/api/v1/areas/{codigo}` | Consultar un área |
| `PUT` | `/api/v1/areas/{codigo}` | Actualizar un área y sus competencias |
| `DELETE` | `/api/v1/areas/{codigo}` | Eliminar un área |
| `POST` | `/api/v1/competencias` | Crear una competencia |
| `GET` | `/api/v1/competencias/{codigo}` | Consultar una competencia |
| `PUT` | `/api/v1/competencias/{codigo}` | Actualizar una competencia y sus áreas |
| `DELETE` | `/api/v1/competencias/{codigo}` | Eliminar una competencia |
| `POST` | `/api/v1/estudiantes` | Crear una cuenta y perfil de estudiante |
| `POST` | `/api/v1/estudiantes/{cuentaUsuarioId}/asignar` | Asignar perfil de estudiante a una cuenta existente |
| `GET` | `/api/v1/secciones` | Listar secciones y resumen de vacantes (solo ADMINISTRADOR) |
| `POST` | `/api/v1/secciones` | Crear sección (solo ADMINISTRADOR) |
| `PATCH` | `/api/v1/secciones/{id}` | Actualizar capacidad de sección (solo ADMINISTRADOR) |
| `POST` | `/api/v1/periodos` | Crear un periodo académico |
| `GET` | `/api/v1/periodos/{periodoId}/vacantes` | Consultar vacantes de un periodo |
| `POST` | `/api/v1/periodos/{periodoId}/matriculas` | Registrar una matrícula |
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
    "cantidadMatriculados": 2
  }
]
```

`cantidadMatriculados` se calcula desde las matrículas: cuenta estudiantes distintos con matrícula activa en estado `EN_REGISTRO` o `CONFIRMADA` asociados a la sección. Las matrículas retiradas, inactivas o finalizadas no cuentan.

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
  "expiresIn": 3600,
  "usuario": {
    "id": 12,
    "username": "usuario@ejemplo.edu.pe",
    "rol": "ADMINISTRADOR",
    "nombreCompleto": "Nombre Apellido",
    "dni": "12345678"
  }
}
```

Credenciales inválidas, cuenta inactiva, bloqueada o datos de login vacíos producen `401` o `400` según corresponda.

#### `GET /api/v1/auth/me`

No recibe parámetros ni cuerpo. Requiere JWT y responde `200` con el objeto `usuario` mostrado arriba (`id`, `username`, `rol`, `nombreCompleto`, `dni`).

#### Recuperación de contraseña

`POST /api/v1/auth/forgot-password` es público y recibe `{ "username": "usuario@ejemplo.edu.pe" }`. Responde con un mensaje genérico tanto si la cuenta existe como si no, para no revelar cuentas registradas. Si corresponde, envía por correo un token aleatorio; en la base se almacena su hash y tiene expiración configurable mediante `PASSWORD_RESET_EXPIRATION_MINUTES` (15 minutos por defecto).

`POST /api/v1/auth/reset-password` es público y recibe `{ "token": "<token-recibido-por-correo>", "password": "nueva-clave-segura" }`. El token es de un solo uso y la contraseña se almacena con BCrypt.

### Cuentas de usuario

`POST /api/v1/cuentas-usuario` y `GET /api/v1/cuentas-usuario` están restringidos al rol `ADMINISTRADOR`. El alta recibe DNI, nombre, apellidos, correo (`username`), contraseña y uno de los roles `ADMINISTRADOR`, `DOCENTE`, `ALUMNO` o `APODERADO`. La contraseña se almacena con BCrypt. La consulta devuelve la lista de cuentas existente; todavía no hay operaciones de edición ni una carga CSV de usuarios.

### Áreas y competencias

Ambos recursos tienen exactamente cuatro operaciones. El `codigo` es una clave de texto de hasta 50 caracteres. La relación entre áreas y competencias es muchos a muchos. Las respuestas exponen los códigos relacionados, no objetos anidados.

#### Áreas

`POST /api/v1/areas` recibe un área y devuelve `201` con el área creada y su encabezado `Location`. `GET /api/v1/areas/{codigo}` devuelve `200`. `PUT /api/v1/areas/{codigo}` reemplaza los datos y relaciones y devuelve `200`. `DELETE /api/v1/areas/{codigo}` elimina el área y devuelve `204` sin cuerpo.

El cuerpo de creación y actualización tiene estos campos:

| Campo | Tipo | Requisito |
| --- | --- | --- |
| `codigo` | string | Obligatorio, no vacío, máximo 50 caracteres; debe coincidir con `{codigo}` en `PUT` |
| `nombre` | string | Obligatorio, no vacío, máximo 255 caracteres |
| `descripcion` | string | Opcional, máximo 255 caracteres |
| `competenciaCodigos` | arreglo de strings | Opcional; códigos de competencias existentes |

Ejemplo:

```json
{
  "codigo": "MAT",
  "nombre": "Matemática",
  "descripcion": "Área curricular de matemática",
  "competenciaCodigos": ["MAT-01", "MAT-02"]
}
```

La respuesta tiene la misma estructura. Si se omite `competenciaCodigos` o se envía vacío al crear/actualizar, el área queda sin competencias. Los códigos referenciados deben existir.

#### Competencias

`POST /api/v1/competencias` recibe una competencia y devuelve `201` con su encabezado `Location`. `GET /api/v1/competencias/{codigo}` devuelve `200`. `PUT /api/v1/competencias/{codigo}` actualiza los datos y relaciones y devuelve `200`. `DELETE /api/v1/competencias/{codigo}` elimina la competencia y devuelve `204` sin cuerpo.

El cuerpo de creación y actualización es equivalente al de área, con `areaCodigos` en lugar de `competenciaCodigos`:

```json
{
  "codigo": "MAT-01",
  "nombre": "Resuelve problemas de cantidad",
  "descripcion": "Descripción opcional",
  "areaCodigos": ["MAT"]
}
```

La respuesta incluye `codigo`, `nombre`, `descripcion` y `areaCodigos`. Omitir o enviar vacía la lista al actualizar quita todas las relaciones. En estas operaciones, un código duplicado genera `409`, un recurso solicitado que no existe genera `404`, un código relacionado inexistente genera `400`, y un código de ruta distinto al del cuerpo en `PUT` genera `400`.

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

DNI o username ya registrados producen `409`.

#### `POST /api/v1/estudiantes/{cuentaUsuarioId}/asignar`

`cuentaUsuarioId` es el ID entero de una cuenta existente. No recibe cuerpo. Asigna el rol `ALUMNO` y crea el perfil de estudiante asociado a la Persona de la cuenta; responde `200` con la misma estructura de `EstudianteResponse`. La cuenta debe existir y estar vinculada a una Persona. Si no existe, responde `404`; si no tiene Persona o ya tiene perfil de estudiante, responde `400` o `409`.

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

`matriculasActivas` cuenta las matrículas activas del periodo cuyo estado no sea `RETIRADA`; `vacantesDisponibles` es el total de la sección menos esas matrículas, con mínimo cero. `hayVacantes` indica si el resultado es mayor a cero. Periodo inexistente responde `404`; capacidad de sección ausente o no válida responde `400`.

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

Periodo o estudiante inexistente responde `404`; falta de vacantes o matrícula activa duplicada responde `409`. Una capacidad no configurada o inválida en la sección responde `400`.

#### `DELETE /api/v1/periodos/{periodoId}/matriculas/{estudianteId}`

`periodoId` y `estudianteId` son enteros de ruta. No recibe cuerpo y responde `204` sin contenido al eliminar la matrícula. Si no existe el periodo o la relación de matrícula, responde `404`.

### Logs de errores

`GET /api/v1/logs/errores` y `GET /api/v1/logs/errores/{id}` están restringidos a `ADMINISTRADOR`. La lista admite filtros opcionales `nivel`, `modulo`, `desde` y `hasta`, fechas ISO `YYYY-MM-DD` y paginación Spring (`page`, `size`, `sort`). El tamaño predeterminado es 20 y el máximo es 100. La lista omite la traza técnica; el detalle la incluye. Las trazas se truncan a 4000 caracteres.

## Estructura

```text
src/main/java/pe/edu/pucp/colegio/
├── ColegioBackendApplication.java
├── common/
│   ├── audit/       # Auditoría y persistencia de errores
│   ├── controller/  # Catálogos, estudiantes, periodos, matrículas y logs
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

Flyway crea las tablas de seguridad, catálogo curricular, matrículas y organización académica. Hibernate valida el esquema al iniciar (`ddl-auto: validate`). Un `Periodo` puede contener varias `Matricula`; cada matrícula enlaza un estudiante con un solo periodo y un estudiante puede matricularse en distintos periodos. Un `Grado` se identifica por número y nivel educativo (`INICIAL`, `PRIMARIA` o `SECUNDARIA`); así se pueden tener, por ejemplo, los grados 1 de Primaria y 1 de Secundaria. Cada grado requiere al menos una `Seccion`, identificada por un código como `A` o `B`, y cada sección pertenece a un solo grado.

## Requisitos

- JDK 25, requerido para compilar y ejecutar el proyecto.
- Maven.
- Un servidor MySQL accesible en `localhost:3306`.

Este checkout todavía no contiene Maven Wrapper ni los workflows de CI descritos en el diseño técnico del repositorio; por ahora se requiere una instalación local de Maven y ejecutar las verificaciones manualmente.

En Fedora, instala Maven con:

> **Despliegue local:** para instalar los requisitos y levantar MySQL + backend + frontend en tu máquina, sigue
> [`docs/despliegue-local.md`](docs/despliegue-local.md). Flujo de ramas y Pull Requests:
> [`docs/flujo-de-trabajo.md`](docs/flujo-de-trabajo.md).

## Configuración
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

El proyecto cuenta con pruebas unitarias y de integración para autenticación, cuentas, catálogo, estudiantes, matrícula, vacantes, autorización y registro de errores. Las pruebas de integración usan Spring Boot, MockMvc y H2; no sustituyen una verificación final contra MySQL.

```bash
mvn test
```

## Alcance y pendientes

El backend aún no cubre todo el Product Backlog. Entre las funcionalidades pendientes de las historias iniciales se encuentran la configuración general del periodo de matrícula según el contrato funcional, la gestión API de grados/secciones y capacidad, las franjas horarias, la autorización completa por rol, la desactivación curricular con protección por notas y la carga masiva CSV de usuarios. La matrícula existente opera con periodos asociados a grado y sección; cualquier integración que espere un periodo único por año escolar debe acordar primero el contrato y el modelo de dominio. Además, la duración JWT predeterminada es de 24 horas y debe revisarse para ajustarse a la decisión de arquitectura de usar tokens de corta duración.

La arquitectura objetivo se describe en el Documento de Arquitectura del proyecto: aplicación Spring Boot monolítica modular, MySQL con migraciones Flyway, frontend SPA independiente y despliegue planeado en AWS. El despliegue en AWS no está configurado en este repositorio.

## Referencias

- [Fedora Project: MySQL Server](https://fedoraproject.org/wiki/Changes/MySQL_9.7)
- [MySQL 8.4: instalación en Windows](https://dev.mysql.com/doc/refman/8.4/en/windows-installation.html)
- [MySQL 8.4: crear una base de datos](https://dev.mysql.com/doc/refman/8.4/en/create-database.html)
