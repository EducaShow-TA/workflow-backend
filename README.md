# EducaShow — backend de autenticación

Backend REST en Spring Boot para el módulo de seguridad de un sistema escolar. Implementa inicio de sesión, autenticación JWT, consulta del perfil autenticado, bloqueo temporal por intentos fallidos y auditoría del inicio de sesión.

## Tecnologías

- Java 17 (`pom.xml`), Spring Boot 3.5.6.
- Spring Web, Validation, Data JPA y Spring Security.
- MySQL para ejecución y pruebas locales.
- H2 para pruebas de integración.
- Flyway para migraciones y JJWT 0.13.0 para JWT.
- Maven y Spotless con Google Java Format.

## Funcionalidades y endpoints

### `POST /api/v1/auth/login`

Recibe `username` y `password`. Valida los campos, comprueba que la cuenta esté activa y no bloqueada, y compara la contraseña con BCrypt. Una contraseña incorrecta incrementa el contador de intentos y puede bloquear la cuenta durante el periodo configurado. Un acceso exitoso reinicia los intentos, registra una auditoría y devuelve un token Bearer con el perfil.

```json
{
  "username": "usuario@ejemplo.edu.pe",
  "password": "contraseña"
}
```

### `GET /api/v1/auth/me`

Requiere `Authorization: Bearer <token>`. Devuelve el perfil asociado al usuario autenticado. La aplicación es stateless; el login es público y las demás rutas requieren autenticación.

### Alta de estudiantes

`POST /api/v1/estudiantes` crea una Persona, su perfil de estudiante y una cuenta nueva con rol `ALUMNO`. La contraseña se guarda con BCrypt; el rol no se recibe desde el cliente.

`POST /api/v1/estudiantes/{cuentaUsuarioId}/asignar` asigna el rol `ALUMNO` a una cuenta existente y crea el perfil de estudiante asociado a la Persona de esa cuenta. La cuenta debe estar vinculada a una Persona y esta no debe tener ya un perfil de estudiante.

### Áreas y competencias

Los endpoints CRUD de `/api/v1/areas` y `/api/v1/competencias` requieren autenticación. Cada recurso admite cuatro operaciones: `POST` para crear, `GET /{codigo}` para consultar, `PUT /{codigo}` para actualizar y `DELETE /{codigo}` para eliminar. El `codigo` es la clave primaria de tipo texto.

La relación es muchos a muchos. Al crear o actualizar un área se puede enviar `competenciaCodigos`; al crear o actualizar una competencia se puede enviar `areaCodigos`. Omitir la lista o enviarla vacía deja el recurso sin relaciones. Las respuestas incluyen los códigos relacionados y no anidan objetos completos.

Ejemplo de área:

```json
{
  "codigo": "MAT",
  "nombre": "Matemática",
  "descripcion": "Área curricular de matemática",
  "competenciaCodigos": ["MAT-01", "MAT-02"]
}
```

## Estructura

```text
src/main/java/pe/edu/pucp/colegio/
├── ColegioBackendApplication.java
├── common/
│   ├── audit/       # Entidades y repositorios de auditoría y errores
│   ├── exception/   # Excepciones de negocio y manejo HTTP global
│   └── model/       # Personas, catálogo curricular y matrículas
└── security/
    ├── config/      # Reglas de acceso y cadena de filtros
    ├── controller/  # Endpoints de autenticación
    ├── dto/         # Solicitudes y respuestas
    ├── jwt/         # Emisión, validación y filtro JWT
    ├── model/       # CuentaUsuario y Rol
    ├── repository/  # Acceso a cuentas
    └── service/     # Lógica de autenticación

src/main/resources/
├── application.yml
└── db/migration/V1__init_security.sql

src/test/
├── java/.../AuthIntegrationTest.java
└── resources/application.yml
```

Flyway crea las tablas de seguridad, catálogo curricular, matrículas y organización académica. Hibernate valida el esquema al iniciar (`ddl-auto: validate`). Un `Periodo` puede contener varias `Matricula`; cada matrícula enlaza un estudiante con un solo periodo y un estudiante puede matricularse en distintos periodos. Un `Grado` se identifica por número y nivel educativo (`INICIAL`, `PRIMARIA` o `SECUNDARIA`); así se pueden tener, por ejemplo, los grados 1 de Primaria y 1 de Secundaria. Cada grado requiere al menos una `Seccion`, identificada por un código como `A` o `B`, y cada sección pertenece a un solo grado.

## Requisitos

- JDK 17 o superior compatible con el nivel configurado en Maven.
- Maven.
- Un servidor MySQL accesible en `localhost:3306`.

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

La migración actual crea una cuenta semilla `admin@educore.edu.pe` y contiene un hash de contraseña conocido por el repositorio. **Úsala solo para pruebas locales y cambia o elimina esa cuenta antes de cualquier despliegue.** En una base donde V1 ya fue aplicada, modificar el archivo V1 no actualiza los datos existentes; la rotación debe hacerse mediante una actualización separada.

Para probar el login en un entorno local recién inicializado:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin@educore.edu.pe","password":"Admin2026!"}'
```

Usa el valor `token` de la respuesta:

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H 'Authorization: Bearer <token>'
```

## Pruebas

La prueba de integración visible usa Spring Boot, MockMvc y H2. Cubre login exitoso del administrador, credenciales incorrectas y rechazo de una ruta protegida sin token. No sustituye una verificación contra el servidor MySQL local.

```bash
mvn test
```

## Observaciones técnicas

- La duración JWT predeterminada es de 24 horas; conviene ajustarla a los requisitos de seguridad.
- El manejador de errores responde con `message`; el punto de entrada de Spring Security construye otra respuesta JSON manual. El contrato puede unificarse.
- Existen entidades y repositorio para `registros_error`, pero el manejador global no persiste allí los errores.
- Las pruebas visibles no cubren bloqueo de cuenta, expiración o firma inválida de token, cuenta inactiva ni validaciones de entrada.
- El POM configura Spotless, pero no liga `spotless:check` a `verify`.
- El proyecto usa el controlador JDBC de MySQL y debe conectarse a MySQL Server.
- El alcance del código disponible se limita al prototipo de seguridad; no se observan módulos escolares de matrícula, pagos u horarios.

## Referencias

- [Fedora Project: MySQL Server](https://fedoraproject.org/wiki/Changes/MySQL_9.7)
- [MySQL 8.4: instalación en Windows](https://dev.mysql.com/doc/refman/8.4/en/windows-installation.html)
- [MySQL 8.4: crear una base de datos](https://dev.mysql.com/doc/refman/8.4/en/create-database.html)
