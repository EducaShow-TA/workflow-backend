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
| Encender Docker (si no arranca solo con el equipo) | `sudo systemctl start docker` |
| Encender | `docker start colegio-mysql` |
| Apagar | `docker stop colegio-mysql` |
| Abrir consola SQL | `docker exec -it colegio-mysql mysql -ucolegio -pcolegio_local colegio_db` |
| Consultar desde tu terminal (con el cliente `mariadb` o `mysql` instalado) | `mariadb -h127.0.0.1 -ucolegio -pcolegio_local colegio_db -e "SHOW TABLES;"` |
| Borrar todo y empezar de cero | `docker rm -f colegio-mysql` y volver a crear el contenedor |

Detener el contenedor o el servicio de Docker no borra los datos; solo `docker rm` los elimina.

No hace falta crear tablas: **Flyway** aplica las migraciones (`src/main/resources/db/migration`) al arrancar el
backend.

## 3. Backend

```bash
git clone git@github.com:EducaShow-TA/workflow-backend.git
cd workflow-backend
git switch <tu-rama>
```

Configuración local con un archivo `.env` (solo la primera vez):

```bash
cp .env.example .env               # Windows: copy .env.example .env
openssl rand -base64 32            # copia el resultado en JWT_SECRET= dentro de .env
```

`.env.example` ya trae los datos del MySQL de la sección 2; solo falta descomentar `JWT_SECRET` y pegar la clave
generada. En Windows sin `openssl`, genera la clave en PowerShell con
`[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))`.

Atajo en Linux: crea `.env` y genera `JWT_SECRET` en un solo paso (en macOS, usa `sed -i ''` en lugar de `sed -i`):

```bash
cp .env.example .env && sed -i "s|^# JWT_SECRET=$|JWT_SECRET=$(openssl rand -base64 32)|" .env
```

Para comprobarlo, `grep '^JWT_SECRET=' .env` debe mostrar una línea sin `#` y con la clave. Si ya tenías un `.env`,
`cp` lo reemplaza: haz una copia antes si guardaste otros valores.

Spring Boot lee `.env` al arrancar (`spring.config.import` en `application.yml`), así que funciona igual desde la
terminal, IntelliJ o NetBeans sin configurar variables en el IDE. `.env` **no se versiona** (está en `.gitignore`):
nunca lo agregues a un commit ni pongas tus secretos en `nbactions.xml` u otros archivos del IDE.

Las variables exportadas en la terminal o definidas en el IDE tienen prioridad sobre `.env`. Sirve para cambiar un
valor puntual sin editar el archivo, por ejemplo `PORT=8081 mvn spring-boot:run`.

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

No hace falta archivo `.env`: con `NUXT_PUBLIC_API_BASE` vacío, el frontend llama a `/api` en el mismo origen y el
`devProxy` de `nuxt.config.ts` reenvía esas peticiones a `http://localhost:8080`. No pongas una URL absoluta como
`http://localhost:3000`: en el navegador de otra persona (por ejemplo, a través de un túnel, sección 6),
`localhost` es su propia máquina.

Los módulos que todavía usan datos de prueba se eligen con `NUXT_PUBLIC_DATOS_PRUEBA` (valor por defecto en
`nuxt.config.ts`, opciones en `.env.example`). Esas pantallas muestran datos simulados y no prueban la integración.
Para forzar que todos los módulos llamen al backend:

```bash
NUXT_PUBLIC_DATOS_PRUEBA= npm run dev
```

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

## 6. Mostrar tu entorno a otros (túnel temporal)

Para enseñar el sistema a tus compañeros por unos minutos, sin desplegarlo, puedes publicar tu `localhost:3000` con
un túnel de Cloudflare. No necesita cuenta.

```bash
# Instalar (una vez)
sudo pacman -S cloudflared          # Arch / CachyOS
winget install Cloudflare.cloudflared   # Windows

# Terminal del frontend: permitir el dominio del túnel en el servidor de desarrollo
__VITE_ADDITIONAL_SERVER_ALLOWED_HOSTS=.trycloudflare.com npm run dev

# Otra terminal: abrir el túnel (imprime una URL https://<palabras>.trycloudflare.com)
cloudflared tunnel --url http://localhost:3000
```

La variable de entorno evita el error `Blocked request. This host ... is not allowed`. Nuxt no lee
`vite.config.js`, así que crear ese archivo no lo soluciona; la variable tampoco modifica ningún archivo versionado.

Antes de compartir la URL:

- **Queda expuesto todo el sistema**, no solo las pantallas: por el `devProxy`, la URL también llega a tu backend
  (`/api/...`) y a tu base local.
- La contraseña del administrador inicial es conocida por todo el equipo. Comparte la URL solo con quienes deban
  verla y cierra el túnel (`Ctrl+C` en la terminal de `cloudflared`) al terminar. Cada túnel nuevo usa una URL
  distinta.
- Lo que hagan los demás queda en tu base local. Para empezar limpio después, recrea el contenedor (sección 2).

## 7. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| Spotless falla con `NoSuchFieldError ... endPositions` | Estás usando un JDK distinto de 25. Revisa `mvn -v` y ajusta `JAVA_HOME`. |
| `failed to connect to the docker API at unix:///var/run/docker.sock` | El servicio de Docker está apagado. Enciéndelo con `sudo systemctl start docker` (o `sudo systemctl enable --now docker` para que arranque con el equipo) y luego `docker start colegio-mysql`. |
| `Communications link failure` al arrancar el backend | MySQL no está encendido o aún no termina de iniciar: `docker start colegio-mysql` y espera a `ready for connections`. |
| Flyway: `Migration checksum mismatch for migration version N` | Tu base aplicó una versión anterior de esa migración y el archivo cambió después. En local, recrea la base (`docker rm -f colegio-mysql` y vuelve a crear el contenedor). En el repositorio, una migración ya integrada no se edita, ni siquiera para cambiar espacios: los cambios van en una `V{n}` nueva. |
| `Could not resolve placeholder 'JWT_SECRET'` | Falta `JWT_SECRET`: créalo en `.env` (sección 3) y comprueba que ejecutas el backend desde la carpeta del repositorio, donde está `.env`. |
| Docker: `port is already allocated` (3306) | Ya tienes otro MySQL/MariaDB en ese puerto. Apágalo o usa `-p 3307:3306` y cambia el puerto en `DB_URL`. |
| La cuenta queda bloqueada al iniciar sesión | Tras 3 intentos fallidos la cuenta se bloquea 15 minutos (US-5.6). Espera o recrea la base. |
| Al iniciar sesión el botón se queda esperando y no pasa nada | El backend está apagado: el `devProxy` no responde mientras `:8080` no esté escuchando. Arráncalo (`mvn spring-boot:run`). |
| `npm warn install-scripts ... blocked` | npm 12 bloquea scripts de instalación de dependencias (`esbuild`, `unrs-resolver`). El proyecto funciona igual; se puede ignorar. |
| Flyway: `MySQL 8.4 is newer than this version of Flyway` | Solo es una advertencia: la versión de Flyway de Spring Boot 3.5 no prueba oficialmente MySQL 8.4. Las migraciones se aplican bien. |
| Maven: `Could not transfer metadata ... flyway-community-db-support ... 401` | Solo es una advertencia: un POM de Flyway declara un repositorio de GitHub Packages que exige autenticación. Las dependencias se descargan de Maven Central y el build no se afecta. |
| Por el túnel: `Blocked request. This host (...) is not allowed` | Levanta el frontend con `__VITE_ADDITIONAL_SERVER_ALLOWED_HOSTS=.trycloudflare.com npm run dev` (sección 6). |
