# Despliegue en AWS

Cómo está desplegado el sistema para las demos de sprint y la exposición final, cómo se encienden y apagan los
servidores, cómo se publica una versión nueva y qué hacer si algo falla. Para trabajar en tu máquina, ver
[`despliegue-local.md`](despliegue-local.md).

```
navegador ──► EC2 colegio-app-server :80  (Nginx)
                 │  /        → SPA (estáticos de workflow-frontend)
                 │  /api/*   → 127.0.0.1:8080 (colegio-backend.jar, systemd)
                 ▼  JDBC (red privada)
              RDS colegio-db  (MySQL 8.4, sin acceso público)

GitHub Actions ──SSH (usuario "despliegue")──► EC2
```

Sigue el Documento de Arquitectura (§5 y §6): el backend se ejecuta como `colegio-backend.jar` con systemd detrás de
Nginx, sin Docker, y el frontend se sirve como estáticos (SPA, sin SSR).

> Este repositorio es público: aquí no van IPs, endpoints, contraseñas ni llaves. Esos datos están en la consola de
> AWS, en el entorno `produccion` de GitHub y en `/etc/colegio/backend.env` dentro del servidor.

## 1. Recursos

Todos están en la región **us-east-2 (Ohio)**.

| Recurso | Nombre | Notas |
|---|---|---|
| VPC | `colegio-vpc` | 2 subredes públicas y 2 privadas, sin NAT Gateway, endpoint S3 de tipo gateway |
| EC2 | `colegio-app-server` | Ubuntu 24.04 (usuario de administración `ubuntu`), Java 25, Nginx. Tiene una IP elástica fija |
| RDS | `colegio-db` | MySQL 8.4 en subred privada; base `colegio_db`; la app se conecta con el usuario `colegio_app` |
| S3 | `colegio-educashow` | Privado, cifrado SSE-S3, solo HTTPS. La EC2 accede con el rol `colegio-ec2-role`, sin llaves |
| Security groups | `colegio-app-sg`, `colegio-db-sg` | EC2: 22, 80 y 443. RDS: 3306 solo desde `colegio-app-sg` |

### En el servidor

| Ruta | Qué es |
|---|---|
| `/etc/colegio/backend.env` | Variables del backend (`DB_URL`, `DB_PASSWORD`, `JWT_SECRET`…). Solo root puede leerlo |
| `/etc/systemd/system/colegio-backend.service` | Servicio del backend; arranca solo al encender la EC2 |
| `/etc/nginx/sites-available/colegio` | Sitio de Nginx: SPA en `/` y proxy de `/api/` al backend |
| `/opt/colegio/colegio-backend.jar` | Enlace al `.jar` de la versión actual |
| `/var/www/colegio` | Enlace a la carpeta del frontend de la versión actual |
| `/opt/colegio/releases/{backend,frontend}/` | Últimas 5 versiones, nombradas por commit |
| `/opt/colegio/bin/desplegar-{backend,frontend}.sh` | Scripts de despliegue (propiedad de root) |
| `/var/log/colegio/colegio-backend.log` | Log del backend (también en `journalctl -u colegio-backend`) |

El reloj de negocio usa la zona `America/Lima`. En `/etc/colegio/backend.env`, configura
`JAVA_TOOL_OPTIONS=-Duser.timezone=America/Lima` para que las llamadas restantes a `LocalDate.now()` y
`LocalDateTime.now()` también usen esa zona, incluso si la EC2 está configurada en UTC. Luego reinicia el servicio
con `sudo systemctl restart colegio-backend`. El cron diario de finanzas declara `America/Lima` directamente.

## 2. Encender y apagar

Los servidores **solo se encienden para las demos**. Con todo apagado se paga únicamente la IP elástica y el
almacenamiento.

**Antes de la demo** (unos 20 minutos antes):

1. **RDS → Databases → `colegio-db` → Actions → Start.** Esperar *Available* (5–10 min). Primero la RDS: el backend
   la necesita al arrancar.
2. Si se va a desplegar una entrega nueva: **RDS → Actions → Take snapshot** (ver §5).
3. **EC2 → Instances → `colegio-app-server` → Instance state → Start.** Esperar *2/2 checks passed*. Nginx y el
   backend arrancan solos.
4. Si hay una entrega nueva en `main`, desplegarla (§3).
5. Abrir la IP elástica en el navegador e iniciar sesión.

**Después de la demo:**

1. **EC2 → Instance state → Stop instance.**
2. **RDS → Actions → Stop temporarily.**

> **AWS vuelve a encender la RDS a los 7 días de detenida.** Entre demos hay que volver a detenerla una vez por
> semana; si no, queda encendida y cobrando.

No liberar la IP elástica: de ella dependen la variable `EC2_HOST` y la huella del servidor guardadas en GitHub.

## 3. Publicar una versión

Los workflows `.github/workflows/backend-deploy.yml` (este repo) y `frontend-deploy.yml`
([`workflow-frontend`](https://github.com/EducaShow-TA/workflow-frontend)) despliegan **solo desde `main`**:

- **Automático:** al integrar la entrega del sprint (`Development` → `main`). Si el servidor está apagado, el workflow
  termina en verde con el aviso *Servidor apagado: despliegue omitido*.
- **Manual:** **Actions → Desplegar backend / Desplegar frontend → Run workflow** (rama `main`). Es lo que se usa el
  día de la demo con el servidor ya encendido. Siempre toma lo último de `main`.

Cada workflow:

1. **Compila y prueba** (`./mvnw verify` en el backend; `npm test` y `npm run generate` en el frontend). Si una
   prueba falla, no despliega.
2. Comprueba que el servidor esté encendido.
3. Copia el artefacto por SSH al servidor y ejecuta el script de despliegue. El backend se reinicia y el script espera
   hasta 120 s a que responda; si no responde, **vuelve solo a la versión anterior** y el workflow queda en rojo.

Desplegar primero el backend y después el frontend, por si la versión nueva del frontend usa endpoints nuevos.

### Acceso

- Puede desplegar cualquier integrante con permiso de escritura en el repo, con el botón *Run workflow*.
- El entorno `produccion` de cada repo guarda la llave SSH del usuario `despliegue` y la huella del servidor
  (`SSH_PRIVATE_KEY`, `SSH_KNOWN_HOSTS`, `EC2_HOST`, `EC2_USER`). Solo la rama `main` puede usarlo.
- El usuario `despliegue` solo puede ejecutar los dos scripts de despliegue con `sudo`; no administra el servidor.
- La administración del servidor (usuario `ubuntu`) requiere la llave `colegio-key.pem`, que no está en el repo.

## 4. Volver a una versión anterior

- **Frontend o backend roto después de desplegar:** en **Actions**, abrir la ejecución del despliegue del commit
  anterior y usar **Re-run all jobs**. Los artefactos se guardan 30 días.
- **El backend nuevo no arrancó:** no hay que hacer nada; el script ya restauró el `.jar` anterior. Revisar el log
  del workflow (incluye las últimas líneas de `journalctl`) y corregir con un PR nuevo.

## 5. Migraciones de Flyway

Volver al `.jar` anterior **no revierte las migraciones**. Si una entrega trae una migración que rompe el arranque,
la versión anterior tampoco arrancará contra la base ya migrada.

- Revisar con especial cuidado las migraciones nuevas en el PR de entrega a `main`.
- **Tomar un snapshot manual de la RDS antes de desplegar una entrega** (§2, paso 2). En el peor caso, se restaura
  ese snapshot.
- Las migraciones ya aplicadas en la RDS son inmutables: las correcciones van en una migración nueva.

## 6. Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| El workflow avisa *Servidor apagado* | La EC2 está detenida | Encenderla y usar *Run workflow* |
| `El backend no respondio en 120 s` | La RDS no estaba lista, o un error de configuración o de Flyway | Revisar el log del workflow; confirmar que la RDS esté *Available* y volver a ejecutar |
| `Communications link failure` en el log | La RDS está apagada o iniciando | Esperar *Available*; luego `sudo systemctl restart colegio-backend` |
| La página carga pero el login da 502 | El backend no está corriendo | `systemctl status colegio-backend` y `journalctl -u colegio-backend -n 100` |
| `Host key verification failed` en el workflow | Cambió la IP o se recreó la EC2 | Actualizar `SSH_KNOWN_HOSTS` (`ssh-keyscan -t ed25519 <IP>`) y `EC2_HOST` en el entorno `produccion` |
| `ssh` da *timeout* desde tu PC | `fail2ban` bloqueó tu IP tras varios intentos fallidos | Esperar 1 hora o conectarte desde otra red |
| GitHub Actions no está disponible | Caída de GitHub | Desplegar a mano: compilar, copiar el artefacto a `~/entrantes/` del usuario `despliegue` y ejecutar el mismo script con el commit |

Logs en el servidor:

```bash
journalctl -u colegio-backend -n 100 --no-pager    # backend
sudo tail -n 50 /var/log/nginx/error.log           # Nginx
sudo fail2ban-client status sshd                    # IPs bloqueadas
```

## 7. Seguridad

- SSH solo con llave (sin contraseñas ni root), `fail2ban` activo y actualizaciones de seguridad automáticas
  (`unattended-upgrades`).
- El backend escucha solo en `127.0.0.1`; desde internet solo se llega por Nginx.
- La RDS no es accesible desde internet.
- Si se filtra la llave de despliegue: borrarla de `/home/despliegue/.ssh/authorized_keys`, generar otra y
  actualizar `SSH_PRIVATE_KEY` en los dos repos.
- Si hay sospecha de intrusión (CPU al 100 %, gasto inusual): quitar las reglas de entrada de `colegio-app-sg`, tomar
  un snapshot del disco, recrear la instancia y cambiar la contraseña de `colegio_app` y el `JWT_SECRET`.
