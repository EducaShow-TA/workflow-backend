# Sistema de ramas y CI — Diseño

- **Fecha:** 2026-10-02
- **Repositorio:** `EducaShow-TA/workflow-backend` (público)
- **Alcance:** sub-proyecto 1 de 2. El sub-proyecto 2 (corrección de incumplimientos del Estándar) tendrá su propia spec.

## 1. Objetivo

Que el equipo (9 integrantes, curso Ingeniería de Software PUCP 2026-2) trabaje en paralelo sin romper el código integrado, con un flujo simple que GitHub haga cumplir automáticamente.

**Criterios de éxito**

- Nadie puede hacer push directo, force-push ni borrar `main` o `develop`.
- Todo cambio entra por Pull Request con 1 aprobación y CI en verde.
- El CI verifica formato (Spotless), compilación, tests, cobertura de la capa de servicio (JaCoCo ≥ 70%) y el formato del título del PR.
- Cada entrega de sprint queda identificada con un tag en `main`.
- Nadie necesita instalar herramientas adicionales para el flujo diario (más allá de JDK 17 + Maven).

**Fuera de alcance:** hooks locales de git, CODEOWNERS / roles de aprobación, ramas `hotfix/`, despliegue continuo a AWS.

## 2. Ramas y flujo

| Rama | Propósito | Recibe PRs desde | Método de merge |
|---|---|---|---|
| `develop` | Integración diaria. **Rama por defecto del repo.** | `feature/*`, `fix/*`, `chore/*` | Solo **squash** |
| `main` | Solo lo entregado al JP. | Solo `develop` | Solo **merge commit** |

- Ramas de trabajo salen de `develop`, en kebab-case, con prefijo del Estándar (`feature/`, `fix/`, `chore/`) y opcionalmente el ID de la historia: `feature/us-5.2-gestion-usuarios`. Deben ser de vida corta (días).
- **Squash hacia `develop`:** cada PR queda como un commit cuyo mensaje es el título del PR (Conventional Commit). Los commits internos de la rama no se validan.
- **Merge commit hacia `main`:** nunca squash; un squash de `develop` → `main` haría divergir el historial y generaría conflictos falsos en el siguiente sprint.
- **Entregas:** tras mergear `develop` → `main`, un integrante crea el tag anotado `vX.Y.0-sprintN` (ej. `v0.1.0-sprint1`) sobre `main`.
- **Correcciones urgentes:** siguen `fix/*` → `develop` → `main`. No hay ramas `hotfix/`.
- **Aprobación:** cualquier integrante puede aprobar, incluido `develop` → `main`. GitHub no permite que el autor apruebe su propio PR.

## 3. Reglas en GitHub (rulesets)

Versionadas como JSON en `.github/rulesets/` e importadas por un admin de la organización desde *Settings → Rules → Rulesets → Import a ruleset*.

| Regla | `develop.json` | `main.json` |
|---|---|---|
| Objetivo | `refs/heads/develop` | `refs/heads/main` |
| `deletion` (bloquear borrado) | sí | sí |
| `non_fast_forward` (bloquear force-push) | sí | sí |
| `pull_request.required_approving_review_count` | 1 | 1 |
| `pull_request.dismiss_stale_reviews_on_push` | true | true |
| `pull_request.required_review_thread_resolution` | true | true |
| `pull_request.allowed_merge_methods` | `["squash"]` | `["merge"]` |
| `required_status_checks` | `build`, `pr-title` | `build`, `pr-title`, `origen-develop` |
| `strict_required_status_checks_policy` | false | false |
| `bypass_actors` | ninguno | ninguno |

**Decisión:** no se exige que la rama esté al día con la base antes de mergear (`strict = false`) para evitar ciclos de actualizar/esperar CI con 9 personas. Si dos PRs válidos por separado rompen `develop` al combinarse, el CI de `push` a `develop` lo detecta y se corrige con un `fix/*`.

**Ajustes manuales del repositorio** (documentados en `CONTRIBUTING.md`):

- Rama por defecto: `develop`.
- *Automatically delete head branches*: activado.
- *Allow rebase merging*: desactivado (squash y merge commit quedan activos; el ruleset restringe cuál aplica en cada rama).

## 4. CI (GitHub Actions)

Dos workflows, solo con acciones oficiales (`actions/checkout`, `actions/setup-java`, `actions/upload-artifact`).

### 4.1 `.github/workflows/ci.yml` — job `build`

- **Disparadores:** `pull_request` (tipos `opened`, `synchronize`, `reopened`) hacia `develop` y `main`; `push` a `develop` y `main`.
- **Pasos:** checkout → JDK 17 Temurin con caché de Maven → `mvn -B verify` (incluye `spotless:check` y `jacoco:check`, ver 4.3) → resumen de cobertura en `$GITHUB_STEP_SUMMARY` (generado con `awk` sobre `target/site/jacoco/jacoco.csv`, `if: always()`) → subir `target/site/jacoco/` como artifact `reporte-cobertura` (`if: always()`).
- Los tests usan H2 en memoria (`src/test/resources/application.yml`), por lo que no se requiere MySQL ni secretos en el CI.

### 4.2 `.github/workflows/pr-checks.yml` — jobs `pr-title` y `origen-develop`

- **Disparador:** `pull_request` (tipos `opened`, `edited`, `synchronize`, `reopened`) hacia `develop` y `main`.
- **`pr-title`:** valida en bash que el título cumpla `^(feat|fix|docs|style|refactor|test|chore)(\([a-z0-9.-]+\))?: .+`. El título se lee desde `github.event.pull_request.title` vía variable de entorno (nunca interpolado directamente en el script, para evitar inyección).
- **`origen-develop`:** con `if: github.base_ref == 'main'`, falla si `github.head_ref != 'develop'`. En PRs hacia `develop` queda omitido; no es check requerido en esa rama.

**Por qué dos workflows:** el evento `edited` (cambio de título) no debe volver a ejecutar `build`. Si `build` estuviera en el mismo workflow con un `if` que lo omite en `edited`, el check omitido ("skipped") sobre el mismo commit reemplazaría a un `build` fallido y GitHub lo contaría como aprobado.

### 4.3 Cambios en `pom.xml`

- **Spotless:** se agrega una ejecución de la meta `check` ligada a la fase `verify`, para que `mvn verify` local sea igual al CI. (El cambio de estilo `GOOGLE` → `AOSP` va en el PR siguiente, ver sección 6.)
- **JaCoCo** `org.jacoco:jacoco-maven-plugin:0.8.15`:
  - `prepare-agent` (fase por defecto).
  - `report` en fase `verify`.
  - `check` en fase `verify` con una regla: elemento `PACKAGE`, `includes` = `pe.edu.pucp.colegio.*.service.impl`, contador `LINE`, `COVEREDRATIO` mínimo `0.70`. Es decir, la capa de servicio de cada módulo debe tener ≥ 70% de líneas cubiertas; DTOs, modelos, configuración y JWT se reportan pero no se evalúan.
- **Línea base medida (2026-10-02, JDK 17):** `seguridad.service.impl` = 86% (31/36 líneas). La regla pasa sin cambios en el código.

## 5. Documentación

- **`.github/pull_request_template.md`** (español): Qué cambia · Historia(s) de usuario (US-x.x) · Cómo probar · Checklist: tests agregados/actualizados, Javadoc en `*Service`/`*Controller`, `mvn spotless:apply` ejecutado, sin secretos ni credenciales, título en formato Conventional Commit.
- **`CONTRIBUTING.md`** (una página, español):
  1. Requisitos: JDK 17 + Maven. Advertencia: google-java-format 1.28 falla con JDK ≥ 25 (`NoSuchFieldError ... endPositions`); usar `JAVA_HOME` apuntando a JDK 17.
  2. Flujo diario con comandos: `git switch develop && git pull` → `git switch -c feature/...` → commits → `mvn spotless:apply && mvn verify` → `git push -u origin feature/...` → abrir PR hacia `develop` con título Conventional Commit.
  3. Revisión: 1 aprobación de otro integrante; resolver comentarios; squash merge.
  4. Entrega de sprint: PR `develop` → `main` (título ej. `chore(release): entrega sprint 1`), merge commit, luego `git tag -a v0.1.0-sprintN -m "Entrega sprint N"` y `git push origin v0.1.0-sprintN`.
  5. Ajustes de repositorio y cómo importar los rulesets (para admins).
- **`README.md`:** sección corta "Cómo contribuir" con enlace a `CONTRIBUTING.md`.

### 5.1 Texto propuesto para el Estándar de Programación (`.docx`, lo edita el equipo)

**Reemplazo de la sección 8.1 (Ramas):**

> Se mantienen dos ramas permanentes: `develop` (integración, rama por defecto) y `main` (versiones entregadas). Las ramas de trabajo se crean desde `develop` con los prefijos `feature/`, `fix/` y `chore/`, y se integran a `develop` mediante Pull Request con *squash merge*. Al cierre de cada sprint se integra `develop` a `main` mediante Pull Request con *merge commit* y se crea el tag `vX.Y.0-sprintN`.

**Reemplazo del último párrafo de 8.2 (Commits):**

> Todo cambio se integra mediante Pull Request con al menos una aprobación de otro integrante y el CI en verde; las reglas de la rama impiden el push directo a `develop` y `main`. El título del Pull Request debe seguir Conventional Commits, ya que se convierte en el mensaje del commit al hacer *squash*.

**Agregado a 7.1 (Pruebas):**

> La cobertura se mide con JaCoCo en `mvn verify`; el build falla si la capa de servicio (`*.service.impl`) de algún módulo baja de 70% de líneas cubiertas.

## 6. Orden de implementación

1. Crear `develop` desde `main` y subirla (antes de activar reglas).
2. En `chore/sistema-ramas-ci` (desde `develop`): `ci.yml`, `pr-checks.yml`, cambios de `pom.xml`, `.github/rulesets/*.json`, plantilla de PR, `CONTRIBUTING.md`, enlace en `README.md` y esta spec. Verificar local con JDK 17: `mvn -B verify`.
3. Abrir PR hacia `develop` con título `chore(ci): agregar sistema de ramas, CI y cobertura`; confirmar los tres checks en verde (`origen-develop` aparece omitido).
4. Admin: importar rulesets, rama por defecto `develop`, borrar ramas tras merge, desactivar rebase merge.
5. Mergear el PR con squash (primera prueba real de las reglas: debe exigir 1 aprobación).
6. PR siguiente `chore/formato-aosp`: Spotless `GOOGLE` → `AOSP` + `mvn spotless:apply` sobre todo el código, antes de que el equipo programe en paralelo. En ese PR, evaluar subir google-java-format a una versión compatible con JDK 25+.

**Verificación del sistema (tras el paso 5):**

- Un push directo a `develop` es rechazado.
- Un PR con título `Agrega login` falla `pr-title`.
- Un PR desde una rama distinta de `develop` hacia `main` falla `origen-develop`.
- Un PR que deja `service.impl` bajo 70% falla `build`. Comprobarlo también en local antes del paso 3 (por ejemplo, subiendo temporalmente el mínimo a `0.99`): si el patrón de `includes` no coincide con ningún paquete, la regla pasa en silencio sin evaluar nada.

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
