# Sistema de ramas y CI — Diseño

- **Fecha:** 2026-10-02
  - v2: adopta el flujo del repositorio de referencia `IngeSoft-Grupo02/Backend`.
  - v3 (2026-10-04): ramas permanentes por integrante (indicación de la jefa de práctica) y JDK 25.
- **Repositorio:** `EducaShow-TA/workflow-backend` (público)
- **Alcance:** sub-proyecto 1 de 2. El sub-proyecto 2 (corrección de incumplimientos del Estándar) tendrá su propia spec.
- **Fuente oficial del flujo de trabajo:** sección 8 del *Estándar de Programación* (`Estandar_de_Programacion_Grupo2.docx`). Esta spec no repite ese flujo; solo define lo necesario para implementarlo y hacerlo cumplir.

## 1. Objetivo

Que el equipo (9 integrantes, curso Ingeniería de Software PUCP 2026-2) trabaje en paralelo con ramas por integrante, siguiendo el flujo del Estándar, y que GitHub haga cumplir automáticamente sus reglas.

**Criterios de éxito**

- Cada integrante trabaja en su rama personal permanente, la actualiza con rebase sobre `Development` y la integra mediante Pull Request con merge commit; `Development` → `main` para presentaciones.
- Nadie puede hacer push directo, force-push ni borrar `main` o `Development`.
- Todo cambio entra por Pull Request con 1 aprobación y CI en verde.
- El CI verifica formato (Spotless), compilación, tests, cobertura de la capa de servicio (JaCoCo ≥ 70%) y el formato de cada commit, y rechaza commits de merge en la rama personal.
- Cada entrega de sprint queda identificada con un tag en `main`.
- No se necesita instalar Maven (Maven Wrapper), solo JDK 25.

**Fuera de alcance:** CD (aún no existe el servidor EC2; ver sección 8), hooks locales de git, CODEOWNERS / roles de aprobación, ramas por tarea o sub-ramas de las ramas personales.

### 1.1 Qué se toma del repositorio de referencia y qué no

| Elemento | Referencia (`IngeSoft-Grupo02/Backend`) | Este diseño |
|---|---|---|
| Ramas base | `main` (presentaciones) + `Development` (integración) | Igual |
| Rama por defecto | `main` | **`Development`** (para que clones y PRs nuevos apunten ahí) |
| Ramas de trabajo | Por tarea, desde `Development`; nombres mixtos (`fix/`, `feature/`, `KS-...`) | **Una rama permanente por integrante** (`nombre-apellido`) |
| Actualización de ramas | `git rebase` diario y antes del PR | Igual; el CI rechaza commits de merge en la rama |
| Integración | PR → `Development` con merge commit | Igual |
| Commits | `tipo: descripción` (Conventional Commits, no validado) | Igual, validado por CI |
| Entregas | PR `Development` → `main`; tag `v1.0.0` en el diagrama | PR `Development` → `main` + tag por sprint |
| Maven Wrapper + `.gitattributes` | Sí | Sí |
| Guía de trabajo | Al inicio de `readme.md` con diagrama | Al inicio de `README.md`, resumen de la sección 8 del Estándar con diagrama Mermaid |
| CI en PRs | No (solo CD; el deploy usa `-DskipTests`) | **Sí** (`build`, `commits`, `origen-development`) |
| Protección de ramas | No | **Sí** (rulesets) |
| CD a EC2 | Docker en cada push a `main` y `Development` | **No por ahora** (sección 8) |

Lecciones del historial de referencia que motivan las diferencias: una rama/PR `KS-disable-test-for-deploy` desactivó tests para poder desplegar; hubo commits directos en `Development`; y ambos `main` y `Development` desplegaban al mismo entorno `production`.

## 2. Ramas

El flujo completo (preparar la rama, rebase diario, commits, push con `--force-with-lease`, PR, actualización tras la integración, conflictos y prácticas no permitidas) está en la sección 8 del Estándar. Resumen de lo que esta spec necesita:

| Rama | Propósito | Recibe PRs desde | Método de merge |
|---|---|---|---|
| `main` | Versión presentada al cierre de cada sprint. | Solo `Development` | Solo **merge commit** |
| `Development` | Integración y prueba. **Rama por defecto.** | Ramas personales | Solo **merge commit** |
| `nombre-apellido` | Rama personal permanente; solo la modifica su dueño. | — | — |

**Ramas personales** (creadas desde `Development`):

| Integrante | Rama |
|---|---|
| Mauricio Villegas Basurco | `mauricio-villegas` |
| Oliver Ramirez Barrantes | `oliver-ramirez` |
| Jose Alonso Chisun Fajardo | `jose-chisun` |
| Diego Alonso La Torre Salas | `diego-la-torre` |
| Dafne Yanoa Janampa Flores | `dafne-janampa` |
| Sebastian Alejandro Morales Cayampe | `sebastian-morales` |
| Renato Sebastian Yallahui Quevedo | `renato-yallahui` |
| Jairo Martín Montoya Anco | `jairo-montoya` |
| Bryan Jair Pisco Tarrillo | `bryan-pisco` |

- No se crean ramas a partir de las ramas personales: cada integrante hace su PR directamente desde su rama hacia `Development`.
- Un PR abierto a la vez por integrante: mientras está abierto, todo push a la rama se suma a ese PR (sección 8.4 del Estándar).
- **Entregas:** PR `Development` → `main` (título `chore(release): entrega sprint N`), merge commit y tag anotado `vX.Y.0-sprintN` sobre `main`.

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

Las ramas personales **no** tienen reglas: su dueño necesita hacer force-push (`--force-with-lease`) después de cada rebase.

**Decisiones**

- `dismiss_stale_reviews_on_push = false`: con rebase diario, cada push invalidaría la aprobación. El CI sigue corriendo en cada push.
- `strict = false`: no se exige que la rama esté al día antes de mergear, para evitar ciclos de actualizar/esperar CI. Si dos PRs válidos por separado rompen `Development` al combinarse, el CI de `push` a `Development` lo detecta y se corrige con un PR desde la rama personal de quien lo arregle.
- `commits` no es requerido en `main` porque en PRs `Development` → `main` ese job no corre (sección 4.2).
- **Opcional, no incluido:** un tercer ruleset que solo bloquee el borrado de las 9 ramas personales (sin bloquear force-push). Al no tener un prefijo común, habría que listar los 9 nombres.

**Ajustes manuales del repositorio** (documentados en el README):

- `Development` como rama por defecto.
- *Automatically delete head branches*: **desactivado** (ya está así; si se activara, GitHub borraría la rama personal tras su primer merge).
- *Allow merge commits*: activado. *Allow squash merging* y *Allow rebase merging*: desactivados.

## 4. CI (GitHub Actions)

Dos workflows, solo con acciones oficiales (`actions/checkout`, `actions/setup-java`, `actions/upload-artifact`).

### 4.1 `.github/workflows/ci.yml` — job `build`

- **Disparadores:** `pull_request` hacia `Development` y `main`; `push` a `Development` y `main`.
- **Pasos:** checkout → JDK 25 Temurin con caché de Maven → `./mvnw -B verify` (incluye `spotless:check` y `jacoco:check`, ver 4.3) → resumen de cobertura en `$GITHUB_STEP_SUMMARY` (generado con `awk` sobre `target/site/jacoco/jacoco.csv`, `if: always()`) → subir `target/site/jacoco/` como artifact `reporte-cobertura` (`if: always()`).
- Los tests usan H2 en memoria (`src/test/resources/application.yml`): no se requiere MySQL ni secretos.

### 4.2 `.github/workflows/pr-checks.yml` — jobs `commits` y `origen-development`

- **Disparador:** `pull_request` (tipos `opened`, `synchronize`, `reopened`) hacia `Development` y `main`. Checkout con `fetch-depth: 0`.
- **`commits`** (`if: github.base_ref == 'Development'`): recorre `git log --format=%s origin/<base>..<head sha>` y falla, listando los culpables, si algún commit:
  - es un commit de merge (`git rev-list --merges` no vacío) → se pide rebase en lugar de merge (sección 8.6 del Estándar);
  - no cumple `^(feat|fix|docs|style|refactor|test|chore)(\([a-z0-9.-]+\))?: .+`.
  Las referencias de rama/SHA se leen desde variables de entorno (`github.base_ref`, `github.event.pull_request.head.sha`), nunca interpoladas directamente en el script.
- **`origen-development`** (`if: github.base_ref == 'main'`): falla si `github.head_ref != 'Development'`.

En PRs hacia `Development`, `origen-development` queda omitido; en PRs hacia `main`, `commits` queda omitido (esos commits ya se validaron al entrar a `Development`). Ninguno de los omitidos es requerido en esa rama.

### 4.3 Cambios de build

- **Maven Wrapper:** generar con `mvn wrapper:wrapper` (tipo `only-script`, sin `maven-wrapper.jar`), versionar `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties`.
- **`.gitattributes`** (igual a la referencia): `/mvnw text eol=lf` y `*.cmd text eol=crlf`.
- **Spotless:** ejecución de la meta `check` ligada a la fase `verify`, para que `./mvnw verify` local sea igual al CI. Se mantienen spotless-maven-plugin 2.44.5 y google-java-format 1.28.0: verificado el 2026-10-04 que funcionan con JDK 25; fallan con JDK 27, y google-java-format 1.37.0 con spotless 3.10.3 también falla. (El cambio de estilo `GOOGLE` → `AOSP` va en un PR posterior, sección 6.)
- **JaCoCo** `org.jacoco:jacoco-maven-plugin:0.8.15` (proyecto de un solo módulo, así que se usa `jacoco:check` directamente, sin el verificador propio de la referencia):
  - `prepare-agent`; `report` en `verify`; `check` en `verify` con regla: elemento `PACKAGE`, `includes` = `pe.edu.pucp.colegio.*.service.impl`, contador `LINE`, `COVEREDRATIO` mínimo `0.70`.
  - Línea base medida (2026-10-02): `seguridad.service.impl` = 86% (31/36 líneas).

## 5. Documentación

- **`README.md`**: se antepone una sección **"Guía de trabajo"** que resume la sección 8 del Estándar: ramas (`main`, `Development`, tabla de ramas personales) → diagrama → flujo diario con comandos (rebase sobre `Development`, `./mvnw spotless:apply && ./mvnw verify`, push con `--force-with-lease`, PR) → entregas (tag) → reglas y CI (qué checks existen, cómo leer el reporte de cobertura) → requisitos (JDK 25; con JDK 27 Spotless falla con `NoSuchFieldError ... endPositions`, usar `JAVA_HOME` apuntando a JDK 25) → ajustes del repositorio para admins. Remite al Estándar como documento oficial. El contenido actual del README (configuración y verificación manual) se conserva debajo, con comandos actualizados a `./mvnw`.
- **Diagrama** en Mermaid (`gitGraph`), renderizado por GitHub: `main`, `Development` y dos ramas personales con commits, rebase sobre `Development`, PRs con merge commit, y merge a `main` con tag de sprint.
- **`.github/pull_request_template.md`** (español): Qué se hizo · Historia(s) de usuario (US-x.x) · Cómo probar · Checklist: rebase sobre `Development` hecho, tests agregados/actualizados, Javadoc en `*Service`/`*Controller`, `./mvnw spotless:apply` ejecutado, sin secretos ni credenciales.
- **Estándar de Programación:** ya actualizado el 2026-10-04 (sección 8 reescrita para ramas por integrante; secciones 3.3 y 4.2 extienden las reglas de protección a `Development`). Pendiente que el equipo agregue a la sección 7.1: *"La cobertura se mide con JaCoCo en `./mvnw verify`; el build falla si la capa de servicio (`*.service.impl`) de algún módulo baja de 70% de líneas cubiertas."*

## 6. Orden de implementación

1. ~~Crear `Development` desde `main` y subirla.~~ Hecho el 2026-10-04.
2. Crear y subir las 9 ramas personales desde `Development`. `bryan-pisco` contiene además los commits de esta spec.
3. En `bryan-pisco`: Maven Wrapper, `.gitattributes`, cambios de `pom.xml`, `ci.yml`, `pr-checks.yml`, `.github/rulesets/*.json`, plantilla de PR y guía en `README.md`. Verificar local con JDK 25: `./mvnw -B verify`, y comprobar que la regla de JaCoCo realmente evalúa `service.impl` (subir temporalmente el mínimo a `0.99` y ver que falla; si el patrón de `includes` no coincide con ningún paquete, la regla pasa en silencio).
4. Abrir PR `bryan-pisco` → `Development` con commits en formato Conventional Commits; confirmar `build` y `commits` en verde.
5. Admin: importar rulesets, rama por defecto `Development`, solo merge commit (el borrado automático de ramas ya está desactivado).
6. Mergear el PR (primera prueba real de las reglas: debe exigir 1 aprobación).
7. PR siguiente, también desde una rama personal: Spotless `GOOGLE` → `AOSP` + `./mvnw spotless:apply` sobre todo el código, antes de que el equipo programe en paralelo.

**Verificación del sistema (tras el paso 6)**

- Un push directo a `Development` es rechazado.
- Un PR con un commit `Agrega login` falla `commits`.
- Un PR cuya rama contiene `Merge branch 'Development' into ...` falla `commits`.
- Un PR desde una rama distinta de `Development` hacia `main` falla `origen-development`.
- Un PR que deja `service.impl` bajo 70% falla `build`.
- Tras el merge, la rama personal sigue existiendo y, después de `git rebase origin/Development`, queda al día.

## 7. Sub-proyecto 2 (siguiente spec)

Corrección de incumplimientos de la revisión del 2026-10-02, en orden de prioridad. Cada ítem va en un PR propio desde la rama personal de quien lo tome:

1. Credenciales del administrador inicial expuestas en `README.md` y `V1__init_security.sql` (repo público; la migración también correría en RDS).
2. Formato de error estándar (`timestamp`, `status`, `error`, `message`, `path`), persistencia de 500 en `RegistroError`, handler de `BusinessException`, 400 para JSON malformado.
3. Pruebas unitarias JUnit 5 + Mockito de `AuthServiceImpl`; nombres `metodo_condicion_resultadoEsperado`.
4. Javadoc en Service/Controller, logger SLF4J en servicios, imports sin comodín, una declaración por línea.
5. Migración `V2`: FK `usuario_id` y CHECK en `accion`, `nivel`, `genero` (V1 no se modifica).
6. Duración del JWT acorde a "corta duración".
7. Identificadores en español.
8. Cambiar `<java.version>` de 17 a 25 en `pom.xml` (el Estándar y la Arquitectura ya indican Java 25; verificado el 2026-10-04 que compila, pasa los tests y arranca con `release 25`).

## 8. CD (pendiente, cuando exista el servidor EC2)

Fuera de este alcance. Cuando se cree la infraestructura, el workflow de despliegue debe:

- Seguir el Documento de Arquitectura (sección 6): `colegio-backend.jar` ejecutado con systemd detrás de Nginx en EC2 — no Docker como en la referencia.
- Desplegar solo desde `main` (o tener un entorno separado para `Development`), a diferencia de la referencia, donde ambas ramas sobrescribían el mismo entorno `production`.
- No usar `-DskipTests`: depender del job `build` ya aprobado.
- Permitir despliegue manual con `workflow_dispatch` en lugar de commits vacíos para forzar el deploy.
