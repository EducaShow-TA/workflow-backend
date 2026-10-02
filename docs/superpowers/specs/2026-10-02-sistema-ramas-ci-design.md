# Sistema de ramas y CI — Diseño

- **Fecha:** 2026-10-02 (v2: adopta el flujo del repositorio de referencia `IngeSoft-Grupo02/Backend`)
- **Repositorio:** `EducaShow-TA/workflow-backend` (público)
- **Alcance:** sub-proyecto 1 de 2. El sub-proyecto 2 (corrección de incumplimientos del Estándar) tendrá su propia spec.

## 1. Objetivo

Que el equipo (9 integrantes, curso Ingeniería de Software PUCP 2026-2) trabaje en paralelo con el mismo flujo de trabajo que el equipo ya usó en `IngeSoft-Grupo02/Backend` (2026-1), agregando las protecciones que ese repositorio no tenía.

**Criterios de éxito**

- El flujo diario es el del repositorio de referencia: ramas desde `Development`, rebase diario, PR hacia `Development` con merge commit, `Development` → `main` para presentaciones.
- Nadie puede hacer push directo, force-push ni borrar `main` o `Development`.
- Todo cambio entra por Pull Request con 1 aprobación y CI en verde.
- El CI verifica formato (Spotless), compilación, tests, cobertura de la capa de servicio (JaCoCo ≥ 70%) y el formato de cada commit.
- Cada entrega de sprint queda identificada con un tag en `main`.
- No se necesita instalar Maven (Maven Wrapper), solo JDK 17.

**Fuera de alcance:** CD (aún no existe el servidor EC2; ver sección 8), hooks locales de git, CODEOWNERS / roles de aprobación, ramas `hotfix/`.

### 1.1 Qué se toma del repositorio de referencia y qué no

| Elemento | Referencia (`IngeSoft-Grupo02/Backend`) | Este diseño |
|---|---|---|
| Ramas base | `main` (presentaciones) + `Development` (integración) | Igual |
| Rama por defecto | `main` | **`Development`** (para que clones y PRs nuevos apunten ahí) |
| Ramas de trabajo | Desde `Development`; nombres mixtos (`fix/`, `feature/`, `KS-...`) | Desde `Development`; prefijos del Estándar `feature/`, `fix/`, `chore/` |
| Actualización de ramas | `git rebase` diario y antes del PR | Igual; el CI rechaza commits de merge en la rama |
| Integración | PR → `Development` con merge commit | Igual |
| Commits | `tipo: descripción` (Conventional Commits, no validado) | Igual, validado por CI |
| Entregas | PR `Development` → `main`; tag `v1.0.0` en el diagrama | PR `Development` → `main` + tag por sprint |
| Maven Wrapper + `.gitattributes` | Sí | Sí |
| Guía de trabajo | Al inicio de `readme.md` con diagrama | Al inicio de `README.md` con diagrama Mermaid |
| CI en PRs | No (solo CD; el deploy usa `-DskipTests`) | **Sí** (`build`, `commits`, `origen-development`) |
| Protección de ramas | No | **Sí** (rulesets) |
| CD a EC2 | Docker en cada push a `main` y `Development` | **No por ahora** (sección 8) |

Lecciones del historial de referencia que motivan las diferencias: una rama/PR `KS-disable-test-for-deploy` desactivó tests para poder desplegar; hubo commits directos en `Development`; y ambos `main` y `Development` desplegaban al mismo entorno `production`.

## 2. Ramas y flujo

| Rama | Propósito | Recibe PRs desde | Método de merge |
|---|---|---|---|
| `Development` | Desarrollo y pruebas antes del producto final. **Rama por defecto.** | `feature/*`, `fix/*`, `chore/*` | Solo **merge commit** |
| `main` | Producto presentado al JP. Todo debe estar probado en `Development`. | Solo `Development` | Solo **merge commit** |

**Flujo del proceso**

1. **Crear la rama de trabajo** desde `Development` actualizada, en kebab-case, con prefijo y opcionalmente el ID de la historia: `feature/us-5.2-gestion-usuarios`.
   ```bash
   git switch Development
   git pull origin Development
   git switch -c feature/us-5.2-gestion-usuarios
   ```
2. **Desarrollar y hacer rebase regularmente** (cada día y cuando se avise que `Development` cambió). Commits frecuentes en formato Conventional Commits.
   ```bash
   git fetch origin
   git rebase origin/Development        # o -i para ordenar/unir commits
   git commit -m "feat(seguridad): agregar bloqueo por intentos fallidos"
   ```
3. **Push y Pull Request** hacia `Development`, describiendo lo hecho para la historia/tarea. Después de un rebase la rama propia se sube con `--force-with-lease` (las reglas solo protegen `main` y `Development`).
   ```bash
   ./mvnw spotless:apply && ./mvnw verify
   git push --force-with-lease -u origin feature/us-5.2-gestion-usuarios
   ```

- **Commits:** `tipo(alcance opcional): descripción`, tipos del Estándar `feat|fix|docs|style|refactor|test|chore`. Se recomienda español, según el Estándar; el CI no valida idioma.
- **Aprobación:** 1 aprobación de cualquier integrante distinto al autor, también para `Development` → `main`.
- **Entregas:** PR `Development` → `main` (título ej. `chore(release): entrega sprint 1`), merge commit, y luego tag anotado `vX.Y.0-sprintN`:
  ```bash
  git switch main && git pull origin main
  git tag -a v0.1.0-sprint1 -m "Entrega sprint 1"
  git push origin v0.1.0-sprint1
  ```
- **Correcciones urgentes:** siguen `fix/*` → `Development` → `main`. No hay ramas `hotfix/`.

## 3. Reglas en GitHub (rulesets)

Versionadas como JSON en `.github/rulesets/` e importadas por un admin de la organización (el autor de esta spec es admin) desde *Settings → Rules → Rulesets → Import a ruleset*.

| Regla | `development.json` | `main.json` |
|---|---|---|
| Objetivo | `refs/heads/Development` | `refs/heads/main` |
| `deletion` (bloquear borrado) | sí | sí |
| `non_fast_forward` (bloquear force-push) | sí | sí |
| `pull_request.required_approving_review_count` | 1 | 1 |
| `pull_request.dismiss_stale_reviews_on_push` | false | false |
| `pull_request.required_review_thread_resolution` | true | true |
| `pull_request.allowed_merge_methods` | `["merge"]` | `["merge"]` |
| `required_status_checks` | `build`, `commits` | `build`, `origen-development` |
| `strict_required_status_checks_policy` | false | false |
| `bypass_actors` | ninguno | ninguno |

**Decisiones**

- `dismiss_stale_reviews_on_push = false`: con rebase diario, cada push invalidaría la aprobación. El CI sigue corriendo en cada push.
- `strict = false`: no se exige que la rama esté al día antes de mergear, para evitar ciclos de actualizar/esperar CI. Si dos PRs válidos por separado rompen `Development` al combinarse, el CI de `push` a `Development` lo detecta y se corrige con un `fix/*`.
- `commits` no es requerido en `main` porque en PRs `Development` → `main` ese job no corre (sección 4.2).

**Ajustes manuales del repositorio** (documentados en el README):

- Crear `Development` y ponerla como rama por defecto.
- *Automatically delete head branches*: activado.
- *Allow merge commits*: activado. *Allow squash merging* y *Allow rebase merging*: desactivados.

## 4. CI (GitHub Actions)

Dos workflows, solo con acciones oficiales (`actions/checkout`, `actions/setup-java`, `actions/upload-artifact`).

### 4.1 `.github/workflows/ci.yml` — job `build`

- **Disparadores:** `pull_request` hacia `Development` y `main`; `push` a `Development` y `main`.
- **Pasos:** checkout → JDK 17 Temurin con caché de Maven → `./mvnw -B verify` (incluye `spotless:check` y `jacoco:check`, ver 4.3) → resumen de cobertura en `$GITHUB_STEP_SUMMARY` (generado con `awk` sobre `target/site/jacoco/jacoco.csv`, `if: always()`) → subir `target/site/jacoco/` como artifact `reporte-cobertura` (`if: always()`).
- Los tests usan H2 en memoria (`src/test/resources/application.yml`): no se requiere MySQL ni secretos.

### 4.2 `.github/workflows/pr-checks.yml` — jobs `commits` y `origen-development`

- **Disparador:** `pull_request` (tipos `opened`, `synchronize`, `reopened`) hacia `Development` y `main`. Checkout con `fetch-depth: 0`.
- **`commits`** (`if: github.base_ref == 'Development'`): recorre `git log --format=%s origin/<base>..<head sha>` y falla, listando los culpables, si algún commit:
  - es un commit de merge (`git rev-list --merges` no vacío) → se pide rebase en lugar de merge;
  - no cumple `^(feat|fix|docs|style|refactor|test|chore)(\([a-z0-9.-]+\))?: .+`.
  Las referencias de rama/SHA se leen desde variables de entorno (`github.base_ref`, `github.event.pull_request.head.sha`), nunca interpoladas directamente en el script.
- **`origen-development`** (`if: github.base_ref == 'main'`): falla si `github.head_ref != 'Development'`.

En PRs hacia `Development`, `origen-development` queda omitido; en PRs hacia `main`, `commits` queda omitido (esos commits ya se validaron al entrar a `Development`). Ninguno de los omitidos es requerido en esa rama.

### 4.3 Cambios de build

- **Maven Wrapper:** generar con `mvn wrapper:wrapper` (tipo `only-script`, sin `maven-wrapper.jar`), versionar `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties`.
- **`.gitattributes`** (igual a la referencia): `/mvnw text eol=lf` y `*.cmd text eol=crlf`.
- **Spotless:** ejecución de la meta `check` ligada a la fase `verify`, para que `./mvnw verify` local sea igual al CI. (El cambio `GOOGLE` → `AOSP` va en el PR siguiente, sección 6.)
- **JaCoCo** `org.jacoco:jacoco-maven-plugin:0.8.15` (proyecto de un solo módulo, así que se usa `jacoco:check` directamente, sin el verificador propio de la referencia):
  - `prepare-agent`; `report` en `verify`; `check` en `verify` con regla: elemento `PACKAGE`, `includes` = `pe.edu.pucp.colegio.*.service.impl`, contador `LINE`, `COVEREDRATIO` mínimo `0.70`.
  - Línea base medida (2026-10-02, JDK 17): `seguridad.service.impl` = 86% (31/36 líneas).

## 5. Documentación

- **`README.md`**: se antepone una sección **"Guía de trabajo"** con la misma estructura que la referencia — Overview (2 ramas base) → `Development` → `main` → diagrama → Flujo del proceso (3 pasos de la sección 2) → Entregas (tag) → Reglas y CI (qué checks existen, cómo leer el reporte de cobertura) → Requisitos (JDK 17; google-java-format 1.28 falla con JDK ≥ 25 con `NoSuchFieldError ... endPositions`, usar `JAVA_HOME` a JDK 17) → Ajustes del repositorio para admins. El contenido actual del README (configuración y verificación manual) se conserva debajo, con comandos actualizados a `./mvnw`.
- **Diagrama** en Mermaid (`gitGraph`), renderizado por GitHub: `main` y `Development`, dos ramas `feature/*` con commits, rebase sobre `Development`, PRs con merge commit, y merge a `main` con tag de sprint.
- **`.github/pull_request_template.md`** (español): Qué se hizo · Historia(s) de usuario (US-x.x) · Cómo probar · Checklist: rebase sobre `Development` hecho, tests agregados/actualizados, Javadoc en `*Service`/`*Controller`, `./mvnw spotless:apply` ejecutado, sin secretos ni credenciales.

### 5.1 Texto propuesto para el Estándar de Programación (`.docx`, lo edita el equipo)

**Reemplazo de la sección 8.1 (Ramas):**

> Se mantienen dos ramas permanentes: `Development` (desarrollo e integración, rama por defecto) y `main` (versiones presentadas). Las ramas de trabajo se crean desde `Development` con los prefijos `feature/`, `fix/` y `chore/`, se actualizan con `git rebase` sobre `Development` diariamente y antes de abrir el Pull Request, y se integran a `Development` mediante Pull Request con *merge commit*. Al cierre de cada sprint se integra `Development` a `main` mediante Pull Request y se crea el tag `vX.Y.0-sprintN`.

**Reemplazo del último párrafo de 8.2 (Commits):**

> Todo cambio se integra mediante Pull Request con al menos una aprobación de otro integrante y el CI en verde; las reglas de la rama impiden el push directo a `Development` y `main`. El CI rechaza commits que no sigan Conventional Commits y commits de merge dentro de la rama de trabajo (se debe usar rebase).

**Agregado a 7.1 (Pruebas):**

> La cobertura se mide con JaCoCo en `./mvnw verify`; el build falla si la capa de servicio (`*.service.impl`) de algún módulo baja de 70% de líneas cubiertas.

## 6. Orden de implementación

1. Crear `Development` desde `main` y subirla (antes de activar reglas).
2. En `chore/sistema-ramas-ci` (desde `Development`): Maven Wrapper, `.gitattributes`, cambios de `pom.xml`, `ci.yml`, `pr-checks.yml`, `.github/rulesets/*.json`, plantilla de PR, guía en `README.md` y esta spec. Verificar local con JDK 17: `./mvnw -B verify`, y comprobar que la regla de JaCoCo realmente evalúa `service.impl` (subir temporalmente el mínimo a `0.99` y ver que falla; si el patrón de `includes` no coincide con ningún paquete, la regla pasa en silencio).
3. Abrir PR hacia `Development` con commits en formato Conventional Commits; confirmar `build` y `commits` en verde.
4. Admin: importar rulesets, rama por defecto `Development`, borrar ramas tras merge, solo merge commit.
5. Mergear el PR (primera prueba real de las reglas: debe exigir 1 aprobación).
6. PR siguiente `chore/formato-aosp`: Spotless `GOOGLE` → `AOSP` + `spotless:apply` sobre todo el código, antes de que el equipo programe en paralelo. Evaluar subir google-java-format a una versión compatible con JDK 25+.

**Verificación del sistema (tras el paso 5)**

- Un push directo a `Development` es rechazado.
- Un PR con un commit `Agrega login` falla `commits`.
- Un PR cuya rama contiene `Merge branch 'Development' into ...` falla `commits`.
- Un PR desde una rama distinta de `Development` hacia `main` falla `origen-development`.
- Un PR que deja `service.impl` bajo 70% falla `build`.

## 7. Sub-proyecto 2 (siguiente spec)

Corrección de incumplimientos de la revisión del 2026-10-02, una rama por ítem, en orden de prioridad:

1. Credenciales del administrador inicial expuestas en `README.md` y `V1__init_security.sql` (repo público; la migración también correría en RDS).
2. Formato de error estándar (`timestamp`, `status`, `error`, `message`, `path`), persistencia de 500 en `RegistroError`, handler de `BusinessException`, 400 para JSON malformado.
3. Pruebas unitarias JUnit 5 + Mockito de `AuthServiceImpl`; nombres `metodo_condicion_resultadoEsperado`.
4. Javadoc en Service/Controller, logger SLF4J en servicios, imports sin comodín, una declaración por línea.
5. Migración `V2`: FK `usuario_id` y CHECK en `accion`, `nivel`, `genero` (V1 no se modifica).
6. Duración del JWT acorde a "corta duración".
7. Identificadores en español.
8. Alinear versión de Java (17 en Estándar/`pom.xml` vs 25 en Arquitectura/`nb-configuration.xml`).

## 8. CD (pendiente, cuando exista el servidor EC2)

Fuera de este alcance. Cuando se cree la infraestructura, el workflow de despliegue debe:

- Seguir el Documento de Arquitectura (sección 6): `colegio-backend.jar` ejecutado con systemd detrás de Nginx en EC2 — no Docker como en la referencia.
- Desplegar solo desde `main` (o tener un entorno separado para `Development`), a diferencia de la referencia, donde ambas ramas sobrescribían el mismo entorno `production`.
- No usar `-DskipTests`: depender del job `build` ya aprobado.
- Permitir despliegue manual con `workflow_dispatch` en lugar de commits vacíos para forzar el deploy.
