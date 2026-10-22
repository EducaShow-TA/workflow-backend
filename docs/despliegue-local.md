# Despliegue local

Guía para levantar el sistema completo en tu máquina: **MySQL** (en Docker) + **backend** (este repositorio) +
**frontend** ([`workflow-frontend`](https://github.com/EducaShow-TA/workflow-frontend)).

```
navegador ──► http://localhost:3000  (Nuxt dev, frontend)
                    │  /api/*  (devProxy de Nuxt)
                    ▼
              http://localhost:8080  (Spring Boot, backend)
                    │  JDBC
                    ▼
              localhost:3306         (MySQL 8.4 en Docker)
```

El frontend llama al API en el **mismo origen** (`/api/...`): en desarrollo el `devProxy` de Nuxt reenvía esas
peticiones al backend, igual que Nginx lo hará en producción. Por eso el backend no necesita CORS.

## 1. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| Git | cualquiera reciente | Clonar los repositorios |
| JDK | **25** (el proyecto es Java 25) | Compilar y ejecutar el backend |
| Maven | 3.9 o superior | Build del backend |
| Node.js + npm | Node 22 LTS | Frontend (Nuxt 4) |
| Docker | Engine o Desktop | Base de datos MySQL 8.4 |

> **Usa JDK 25, no uno más nuevo.** Con JDK 27, Spotless falla con `NoSuchFieldError ... endPositions`.
> Si tienes varios JDK, apunta `JAVA_HOME` al 25 antes de usar Maven.

### Arch Linux / CachyOS

```bash
sudo pacman -S --needed git jdk25-openjdk maven nodejs-lts-jod npm docker
sudo archlinux-java set java-25-openjdk     # JDK 25 por defecto
sudo systemctl enable --now docker           # inicia Docker (y al arrancar el equipo)
```

### Ubuntu / Debian

```bash
sudo apt update
sudo apt install -y git maven docker.io
sudo systemctl enable --now docker

# JDK 25: si tu versión de Ubuntu no trae openjdk-25-jdk, instálalo con SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-tem

# Node 22 con nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash
source ~/.bashrc
nvm install 22
```

### Windows (PowerShell)

```powershell
winget install Git.Git
winget install EclipseAdoptium.Temurin.25.JDK
winget install OpenJS.NodeJS.LTS
winget install Docker.DockerDesktop
```

Maven: descarga el binario de <https://maven.apache.org/download.cgi>, descomprímelo y agrega su carpeta `bin` al
`PATH`. Abre Docker Desktop antes de seguir.

### Comprobar la instalación

```bash
git --version
java -version      # debe decir 25
mvn -v             # "Java version: 25..."
node -v            # v22.x
npm -v
docker --version
```

> En Linux, los comandos `docker` necesitan `sudo` salvo que tu usuario esté en el grupo `docker`
> (`sudo usermod -aG docker $USER` y volver a iniciar sesión). Ese grupo equivale a permisos de root.

## 2. Base de datos (MySQL 8.4 en Docker)

Crear el contenedor (solo la primera vez; descarga unos 500 MB):

```bash
docker run -d --name colegio-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root_local \
  -e MYSQL_DATABASE=colegio_db \
  -e MYSQL_USER=colegio \
  -e MYSQL_PASSWORD=colegio_local \
  mysql:8.4
```

Estas credenciales son **solo para tu máquina**. Nunca uses contraseñas reales ni las subas al repositorio.

Esperar a que esté lista (unos 20 segundos):

```bash
docker logs colegio-mysql 2>&1 | grep "port: 3306"    # "ready for connections ... port: 3306"
```

Uso diario:

| Acción | Comando |
|---|---|
| Encender | `docker start colegio-mysql` |
| Apagar | `docker stop colegio-mysql` |
| Abrir consola SQL | `docker exec -it colegio-mysql mysql -ucolegio -pcolegio_local colegio_db` |
| Borrar todo y empezar de cero | `docker rm -f colegio-mysql` y volver a crear el contenedor |

No hace falta crear tablas: **Flyway** aplica las migraciones (`src/main/resources/db/migration`) al arrancar el
backend.

## 3. Backend

```bash
git clone git@github.com:EducaShow-TA/workflow-backend.git
cd workflow-backend
git switch <tu-rama>
```

Variables de entorno (en cada terminal donde ejecutes el backend):

```bash
# Linux / macOS
export DB_URL='jdbc:mysql://localhost:3306/colegio_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USERNAME='colegio'
export DB_PASSWORD='colegio_local'
export JWT_SECRET="$(openssl rand -base64 32)"
```

```powershell
# Windows (PowerShell)
$env:DB_URL = 'jdbc:mysql://localhost:3306/colegio_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:DB_USERNAME = 'colegio'
$env:DB_PASSWORD = 'colegio_local'
$env:JWT_SECRET = [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

En un IDE, defínelas en la configuración de ejecución (IntelliJ: *Run → Edit Configurations → Environment
variables*). En NetBeans quedan en `nbactions.xml`, que **no se versiona**.

Verificar y arrancar:

```bash
mvn test              # ejecuta las pruebas; usan H2 y no requieren MySQL
mvn spotless:check    # comprueba el formato Java
mvn spring-boot:run   # arranca en http://localhost:8080
```

Debe aparecer `Successfully applied N migration(s)` (solo la primera vez) y `Started ColegioBackendApplication`.

Comprobar el API (con la contraseña del administrador inicial, que te dará el equipo):

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin@educore.edu.pe","password":"<contrasenia>"}'
```

Responde `200` con `token` y `usuario`.

## 4. Frontend

En otra terminal, con el backend encendido:

```bash
git clone git@github.com:EducaShow-TA/workflow-frontend.git
cd workflow-frontend
git switch <tu-rama>
npm ci             # instala exactamente lo de package-lock.json
npm run dev        # http://localhost:3000
```

El frontend de este checkout activa datos de demostración cuando `NUXT_PUBLIC_API_BASE` está vacío. Para probar el
backend local mediante el `devProxy` configurado en `nuxt.config.ts`, crea un archivo `.env` en el frontend con:

```dotenv
NUXT_PUBLIC_API_BASE=http://localhost:3000
```

Así las solicitudes van al mismo origen del frontend y Nuxt reenvía `/api` a `http://localhost:8080`. Si se omite
esta variable, las pantallas que usan `useDatosPrueba()` muestran datos simulados y no prueban la integración.

Verificaciones antes de abrir un Pull Request:

```bash
npx eslint .
npx prettier --check .
npx nuxi typecheck
```

## 5. Prueba de punta a punta

Para recorrer las APIs del backend con Postman, importar la colección y el environment, y conocer sus precondiciones y datos de prueba, sigue [`../postman/README.md`](../postman/README.md). La colección debe apuntar a una base local de pruebas aislada; no la ejecutes contra una base con datos que quieras conservar.

1. Abre <http://localhost:3000/login>.
2. Ingresa con el administrador inicial → debes llegar a `/panel`.
3. El menú de perfil (arriba a la derecha) muestra el nombre del usuario.
4. "Cerrar sesión" vuelve al login.
5. Una contraseña incorrecta muestra "Credenciales inválidas".

Al recargar la página se pide iniciar sesión otra vez: el token se guarda solo en memoria (comportamiento esperado).

## 6. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| Spotless falla con `NoSuchFieldError ... endPositions` | Estás usando un JDK distinto de 25. Revisa `mvn -v` y ajusta `JAVA_HOME`. |
| `Communications link failure` al arrancar el backend | MySQL no está encendido o aún no termina de iniciar: `docker start colegio-mysql` y espera a `ready for connections`. |
| `Could not resolve placeholder 'JWT_SECRET'` | Falta la variable de entorno `JWT_SECRET` en esa terminal o en la configuración del IDE. |
| Docker: `port is already allocated` (3306) | Ya tienes otro MySQL/MariaDB en ese puerto. Apágalo o usa `-p 3307:3306` y cambia el puerto en `DB_URL`. |
| La cuenta queda bloqueada al iniciar sesión | Tras 3 intentos fallidos la cuenta se bloquea 15 minutos (US-5.6). Espera o recrea la base. |
| Al iniciar sesión el botón se queda esperando y no pasa nada | El backend está apagado: el `devProxy` no responde mientras `:8080` no esté escuchando. Arráncalo (`mvn spring-boot:run`). |
| `npm warn install-scripts ... blocked` | npm 12 bloquea scripts de instalación de dependencias (`esbuild`, `unrs-resolver`). El proyecto funciona igual; se puede ignorar. |
| Flyway: `MySQL 8.4 is newer than this version of Flyway` | Solo es una advertencia: la versión de Flyway de Spring Boot 3.5 no prueba oficialmente MySQL 8.4. Las migraciones se aplican bien. |
