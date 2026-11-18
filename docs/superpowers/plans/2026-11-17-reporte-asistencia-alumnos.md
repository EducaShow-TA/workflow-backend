# Reporte de inasistencias y tardanzas de alumnos (US-2.6) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidado por alumno de presentes, tardanzas, inasistencias y justificadas en un rango de fechas,
filtrable por grado y sección, visible y exportable a CSV por administradores y docentes.

**Architecture:** Una consulta JPQL agregada (`GROUP BY` estudiante y sección) en
`AsistenciaEstudianteRepository` aplica dentro de la misma consulta la regla de acceso del docente del
historial. `ReporteAsistenciaService` valida, calcula porcentajes, totales y orden; `ReporteAsistenciaCsv`
serializa; `ReporteAsistenciaController` expone tres rutas. En el frontend, un composable y un componente
compartido montado en una página de administrador y otra de docente, con paginación en el cliente.

**Tech Stack:** Java 25, Spring Boot 3.5.6, Spring Data JPA, commons-csv, JUnit 5 + Mockito, `@SpringBootTest`
sobre H2 (`MODE=MySQL`); Nuxt 4 SPA + Vue 3 + TypeScript estricto, Vitest.

**Spec:** `workflow-backend/docs/superpowers/specs/2026-11-17-reporte-asistencia-alumnos-design.md`

## Global Constraints

- Rama `bryan-pisco` en `workflow-backend` y `workflow-frontend`. **Sin commits ni push** (indicación del
  integrante): cada tarea termina con los cambios en el árbol de trabajo.
- Backend con JDK 25 (`JAVA_HOME` a JDK 25; con JDK 27 Spotless falla). Usar `sh ./mvnw`, nunca `sudo`.
- Código nuevo en `pe.edu.pucp.colegio.common.{controller,dto,repository,service}` (patrón actual de asistencia).
- Identificadores en español sin tildes ni ñ. DTOs `record` con sufijo `*Response`.
- `*Service` y `*Controller` con Javadoc; SLF4J en el servicio; sin `System.out`.
- Sin migraciones Flyway. Sin dependencias nuevas (commons-csv ya está en el `pom.xml`).
- Rutas: `/api/v1/reportes/asistencia-estudiantes`, `/api/v1/reportes/asistencia-estudiantes/csv`,
  `/api/v1/reportes/asistencia-estudiantes/secciones`. Roles `ADMINISTRADOR` y `DOCENTE`.
- `PENDIENTE_MARCAJE` no cuenta. `porcentajeInasistencia = inasistencias * 100 / totalSesiones`, 1 decimal,
  `HALF_UP`, 0 si `totalSesiones = 0`. Orden: % desc, inasistencias desc, `nombreCompleto` asc.
- Errores: `desde > hasta` → `BusinessException` (422); docente con sección ajena →
  `ResponseStatusException(HttpStatus.FORBIDDEN)`; parámetros faltantes o mal formados → 400 (ya lo hace
  `GlobalExceptionHandler`).
- CSV: BOM UTF-8 (`﻿`), `CSVFormat.DEFAULT`, encabezado exacto
  `Alumno,Grado,Seccion,Presentes,Tardanzas,Inasistencias,Justificadas,Total sesiones,% inasistencia`,
  última fila `TOTAL`. Archivo `reporte-asistencia_{desde}_{hasta}.csv`.
- Etiqueta de grado: `"{numero}.° {Nivel}"` con `Inicial`, `Primaria`, `Secundaria`.
- Frontend: sin llamadas HTTP en `.vue`, `<script setup lang="ts">`, sin `any`, 2 espacios; pruebas que dependen de
  hoy usan `vi.useFakeTimers()` + `vi.setSystemTime()`. npm ≥ 11.
- Umbral de resaltado en la tabla: constante `UMBRAL_RESALTADO = 30` (%). Paginación cliente: 20 filas.

## Review Focus

1. **Rango de un solo día** (`desde == hasta`): las sesiones de ese día cuentan (límites inclusivos). → Tarea 1.
2. **Alumno que cambió de sección dentro del rango**: dos filas, una por sección, sin mezclar conteos. → Tarea 1.
3. **Docente filtra por un `gradoId` donde no enseña, sin `seccionId`**: lista vacía con 200, no 403. → Tarea 2.
4. **Nombres con tildes, ñ o comas en el CSV**: bytes empiezan con `EF BB BF`, `Muñoz` intacto, comas entre
   comillas. → Tarea 3.
5. **Cambiar el grado en la pantalla deja una sección de otro grado seleccionada**: al cambiar `gradoId` se
   limpia `seccionId` si no pertenece al nuevo grado. → Tarea 5 (`seccionesDelGrado` + prueba).

---

### Task 1: Consulta agregada y catálogo de secciones en repositorios

**Files:**
- Create: `workflow-backend/src/main/java/pe/edu/pucp/colegio/common/repository/ConsolidadoAsistenciaFila.java`
- Modify: `workflow-backend/src/main/java/pe/edu/pucp/colegio/common/repository/AsistenciaEstudianteRepository.java`
- Modify: `workflow-backend/src/main/java/pe/edu/pucp/colegio/common/repository/SeccionRepository.java`
- Modify: `workflow-backend/src/main/java/pe/edu/pucp/colegio/common/repository/SesionClaseRepository.java`
- Test: `workflow-backend/src/test/java/pe/edu/pucp/colegio/common/ReporteAsistenciaIntegrationTest.java`

**Interfaces:**
- Produces:
  - `interface ConsolidadoAsistenciaFila { Integer getEstudianteId(); String getNombre(); String getApellidoPaterno(); String getApellidoMaterno(); Integer getGradoId(); Integer getGradoNumero(); NivelEducativo getNivelEducativo(); Integer getSeccionId(); String getSeccionCodigo(); Long getPresentes(); Long getTardanzas(); Long getInasistencias(); Long getJustificadas(); }`
  - `List<ConsolidadoAsistenciaFila> AsistenciaEstudianteRepository.consolidarPorAlumno(LocalDate desde, LocalDate hasta, Integer gradoId, Integer seccionId, Integer personaIdDocente)` — `personaIdDocente == null` para administrador.
  - `boolean SesionClaseRepository.existsByDocentePersonaIdAndSeccionIdAndFechaBetweenAndActiveTrue(Integer personaId, Integer seccionId, LocalDate desde, LocalDate hasta)`
  - `List<Seccion> SeccionRepository.buscarSeccionesDelDocente(Integer personaId)` — con `join fetch s.grado`.

- [ ] **Step 1: Escribir las pruebas de integración del repositorio**

Clase `@SpringBootTest @Transactional` con fixtures vía `EntityManager`, copiando el patrón de
`AsistenciaRestIntegrationTest.crearFixture()` (Persona, Estudiante, Docente, Area, Grado con número único de un
`AtomicInteger` desde `50_000`, Seccion, SesionClase, `AsistenciaEstudiante` con `marcarPresente()` /
`marcarTardanza()` / `marcarAusente()` / `justificar("motivo", null)`). Fecha base `LocalDate.of(2026, 11, 4)`.

```java
@Test void consolidarPorAlumno_variosEstados_cuentaCadaEstadoYExcluyePendiente()
// 1 PRESENTE, 2 TARDANZA, 1 AUSENTE, 1 JUSTIFICADA, 1 PENDIENTE_MARCAJE del mismo alumno
// assert fila: presentes 1, tardanzas 2, inasistencias 1, justificadas 1

@Test void consolidarPorAlumno_fueraDeRangoOInactivo_noCuenta()
// sesión un día antes de desde, sesión con active=false, registro con active=false → no suman

@Test void consolidarPorAlumno_rangoDeUnDia_incluyeLimites()
// desde == hasta == fecha de la sesión → la fila aparece con 1

@Test void consolidarPorAlumno_alumnoCambiaDeSeccion_devuelveUnaFilaPorSeccion()
// mismo estudiante, sesiones en sección A y B → 2 filas con seccionId distintos

@Test void consolidarPorAlumno_filtroGradoYSeccion_restringe()
// gradoId de otro grado → vacío; seccionId correcto → solo esa sección

@Test void consolidarPorAlumno_docente_veSuSeccionCompletaPeroNoSeccionesAjenas()
// docente D1 tiene SesionHorario vigente en sección A (horario activo); otra área de A dictada por D2 → visible;
// sección B sin clase de D1 → no aparece. Docente que solo dicta una SesionClase sin SesionHorario → la ve.

@Test void buscarSeccionesDelDocente_horarioOSesionDictada_devuelveSinDuplicados()
```

Para `SesionHorario` / `HorarioSeccion` / `PlantillaHorario` / `BloqueHorario` / `Aula` reutilizar cómo los crea
`AsistenciaSesionClaseIntegrationTest` (o el test de horarios existente que persista `SesionHorario`).

- [ ] **Step 2: Correr y verificar que fallan**

Run: `cd workflow-backend && sh ./mvnw -q test -Dtest=ReporteAsistenciaIntegrationTest`
Expected: error de compilación (`consolidarPorAlumno` no existe).

- [ ] **Step 3: Implementar la projection y los métodos**

`consolidarPorAlumno` en JPQL con alias iguales a los getters de la projection:

```
select e.id as estudianteId, p.nombre as nombre, p.apellidoPaterno as apellidoPaterno,
       p.apellidoMaterno as apellidoMaterno, g.id as gradoId, g.numero as gradoNumero,
       g.nivelEducativo as nivelEducativo, sec.id as seccionId, sec.codigo as seccionCodigo,
       sum(case when a.estadoAsistencia = PRESENTE then 1 else 0 end) as presentes, ... (TARDANZA, AUSENTE, JUSTIFICADA)
from AsistenciaEstudiante a join a.estudiante e join e.persona p join a.sesionClase s
     join s.seccion sec join sec.grado g
where a.active = true and s.active = true and s.fecha between :desde and :hasta
  and a.estadoAsistencia <> PENDIENTE_MARCAJE
  and (:gradoId is null or g.id = :gradoId) and (:seccionId is null or sec.id = :seccionId)
  and (:personaId is null or s.docente.persona.id = :personaId or exists (
        select sh.id from SesionHorario sh where sh.docente.persona.id = :personaId
          and sh.horario.seccion = sec and sh.horario.active = true
          and sh.vigenteDesde <= s.fecha and (sh.vigenteHasta is null or sh.vigenteHasta >= s.fecha)))
group by e.id, p.nombre, p.apellidoPaterno, p.apellidoMaterno, g.id, g.numero, g.nivelEducativo, sec.id, sec.codigo
```

Los literales de enum van con su nombre completo (`pe.edu.pucp.colegio.common.model.EstadoAsistencia.PRESENTE`).
`buscarSeccionesDelDocente`: `select distinct s from Seccion s join fetch s.grado where s.active = true and
(exists SesionHorario de horario activo con ese docente en s or exists SesionClase activa de ese docente en s)`.

- [ ] **Step 4: Correr y verificar que pasan**

Run: `cd workflow-backend && sh ./mvnw -q test -Dtest=ReporteAsistenciaIntegrationTest`
Expected: `Tests run: 7, Failures: 0, Errors: 0`. Si H2 rechaza algo de la consulta, ajustarla de modo que siga
siendo JPQL válido también para MySQL (no usar SQL nativo).

- [ ] **Step 5: Sin commit** (indicación del integrante).

---

### Task 2: DTOs y `ReporteAsistenciaService`

**Files:**
- Create: `.../common/dto/ReporteAsistenciaResponse.java`, `TotalesAsistenciaResponse.java`,
  `FilaReporteAsistenciaResponse.java`, `SeccionReporteAsistenciaResponse.java`
- Create: `.../common/service/ReporteAsistenciaService.java`
- Test: `workflow-backend/src/test/java/pe/edu/pucp/colegio/common/service/ReporteAsistenciaServiceTest.java`

**Interfaces:**
- Consumes: Tarea 1 (todos sus métodos); `CuentaUsuarioRepository.findByUsername(String)`;
  `SesionClaseRepository.docenteTieneClaseEnSeccion(personaId, seccionId, desde, hasta)`;
  `SeccionRepository.findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc()`.
- Produces:
  - `record TotalesAsistenciaResponse(long presentes, long tardanzas, long inasistencias, long justificadas, long totalSesiones)`
  - `record FilaReporteAsistenciaResponse(Integer estudianteId, String nombreCompleto, Integer gradoId, String grado, Integer seccionId, String seccion, long presentes, long tardanzas, long inasistencias, long justificadas, long totalSesiones, BigDecimal porcentajeInasistencia)`
  - `record ReporteAsistenciaResponse(LocalDate desde, LocalDate hasta, TotalesAsistenciaResponse totales, List<FilaReporteAsistenciaResponse> alumnos)`
  - `record SeccionReporteAsistenciaResponse(Integer seccionId, String seccion, Integer gradoId, String grado)`
  - `ReporteAsistenciaResponse ReporteAsistenciaService.generar(LocalDate desde, LocalDate hasta, Integer gradoId, Integer seccionId, String username)`
  - `List<SeccionReporteAsistenciaResponse> ReporteAsistenciaService.listarSecciones(String username)`
  - `static BigDecimal ReporteAsistenciaService.porcentaje(long inasistencias, long totalSesiones)` (paquete-privado o público; la usa también la Tarea 3)

- [ ] **Step 1: Escribir las pruebas unitarias** (Mockito; repositorios y `CuentaUsuario` mockeados; filas de
  la projection como mocks o una implementación mínima en la prueba)

```java
generar_rangoInvertido_lanzaBusinessException()      // desde 2026-11-10, hasta 2026-11-01
generar_administrador_noFiltraPorDocente()            // verify consolidarPorAlumno(..., null)
generar_docenteConSeccionAjena_lanza403()             // docenteTieneClaseEnSeccion=false y exists...=false → status 403
generar_docenteConSesionDictadaSinHorario_permite()   // docenteTieneClaseEnSeccion=false, exists...=true → OK
generar_docenteSinSeccion_filtraPorSuPersona()        // verify consolidarPorAlumno(..., personaId)
generar_docenteConGradoAjenoSinSeccion_devuelveVacio() // repositorio devuelve []; no lanza; totales en 0
generar_alumnoOApoderado_lanza403()
generar_sinSesionesContables_porcentajeCero()         // porcentaje(0, 0) == 0.0
generar_variasFilas_calculaPorcentajeYTotales()       // 6 de 52 → 11.5; totales sumados
generar_variosAlumnos_ordenaPorPorcentajeDescYNombre() // empate de % e inasistencias → nombre asc
generar_fila_armaEtiquetasDeGradoYNombre()            // 3 + SECUNDARIA → "3.° Secundaria"; "Ana Quispe Rojas"
listarSecciones_administrador_ordenaPorNivelNumeroYCodigo()
listarSecciones_docente_usaBuscarSeccionesDelDocente()
```

- [ ] **Step 2: Correr y verificar que fallan**

Run: `cd workflow-backend && sh ./mvnw -q test -Dtest=ReporteAsistenciaServiceTest`
Expected: error de compilación (clases inexistentes).

- [ ] **Step 3: Implementar DTOs y servicio**

`@Service`, `@Transactional(readOnly = true)` en los métodos públicos, Javadoc de clase. Rol desde
`CuentaUsuario.getRol()`: ADMINISTRADOR → `personaId = null`; DOCENTE → `cuenta.getPersona().getId()`; otro →
403. Validación del docente con `seccionId`: permitido si `docenteTieneClaseEnSeccion(...)` **o**
`existsByDocentePersonaIdAndSeccionIdAndFechaBetweenAndActiveTrue(...)`. `log.info` con usuario, rango y filtros
(sin datos de alumnos).

- [ ] **Step 4: Correr y verificar que pasan**

Run: `cd workflow-backend && sh ./mvnw -q test -Dtest=ReporteAsistenciaServiceTest`
Expected: `Tests run: 13, Failures: 0, Errors: 0`

- [ ] **Step 5: Sin commit.**

---

### Task 3: Serialización CSV

**Files:**
- Create: `.../common/service/ReporteAsistenciaCsv.java`
- Test: `workflow-backend/src/test/java/pe/edu/pucp/colegio/common/service/ReporteAsistenciaCsvTest.java`

**Interfaces:**
- Consumes: DTOs y `ReporteAsistenciaService.porcentaje` de la Tarea 2.
- Produces: `@Component ReporteAsistenciaCsv` con `byte[] exportar(ReporteAsistenciaResponse reporte)` (UTF-8).

- [ ] **Step 1: Escribir las pruebas**

```java
exportar_reporte_incluyeBomEncabezadoFilasYTotal()
// bytes[0..2] == EF BB BF; línea 1 == encabezado exacto de Global Constraints;
// fila "Ana Quispe Rojas,3.° Secundaria,A,40,5,6,1,52,11.5"; última fila "TOTAL,,,40,5,6,1,52,11.5"
exportar_nombreConComaYEnie_loEscapaSinPerderCaracteres()
// nombre "Muñoz, Pedro" → aparece como "\"Muñoz, Pedro\"" y el texto decodificado en UTF-8 contiene "Muñoz"
exportar_sinAlumnos_soloEncabezadoYTotalEnCero()
```

- [ ] **Step 2: Correr y verificar que fallan** — `sh ./mvnw -q test -Dtest=ReporteAsistenciaCsvTest` → compilación falla.
- [ ] **Step 3: Implementar** con `CSVPrinter` sobre un `OutputStreamWriter(UTF_8)` que escribe primero `﻿`;
  el % se escribe con `toPlainString()`; separador de registros `\r\n` (default de `CSVFormat.DEFAULT`).
- [ ] **Step 4: Correr y verificar que pasan** — Expected: `Tests run: 3, Failures: 0`.
- [ ] **Step 5: Sin commit.**

---

### Task 4: `ReporteAsistenciaController` y pruebas REST

**Files:**
- Create: `.../common/controller/ReporteAsistenciaController.java`
- Modify (test): `workflow-backend/src/test/java/pe/edu/pucp/colegio/common/ReporteAsistenciaIntegrationTest.java`

**Interfaces:**
- Consumes: `ReporteAsistenciaService.generar`, `listarSecciones` (Tarea 2); `ReporteAsistenciaCsv.exportar` (Tarea 3).
- Produces (contrato REST que consume el frontend):
  - `GET /api/v1/reportes/asistencia-estudiantes?desde&hasta[&gradoId][&seccionId]` → `ReporteAsistenciaResponse`
  - `GET /api/v1/reportes/asistencia-estudiantes/csv?…` → `text/csv;charset=UTF-8`,
    `Content-Disposition: attachment; filename="reporte-asistencia_{desde}_{hasta}.csv"`
  - `GET /api/v1/reportes/asistencia-estudiantes/secciones` → `List<SeccionReporteAsistenciaResponse>`

- [ ] **Step 1: Agregar pruebas REST** (MockMvc, `with(user(usuario).roles(...))` como `AsistenciaRestIntegrationTest`)

```java
reporte_administrador_devuelveConsolidadoConTotales()   // jsonPath $.totales.totalSesiones, $.alumnos[0].porcentajeInasistencia
reporte_sinDesde_responde400()
reporte_rangoInvertido_responde422()                    // jsonPath $.status == 422 (JSON estándar)
reporte_docenteSeccionAjena_responde403()
reporte_alumno_responde403()
csv_administrador_respondeTextCsvConAdjunto()           // content type text/csv, header Content-Disposition con el nombre
secciones_docente_soloSusSecciones()
```

- [ ] **Step 2: Correr y verificar que fallan** — `sh ./mvnw -q test -Dtest=ReporteAsistenciaIntegrationTest` → 404 en las rutas nuevas.
- [ ] **Step 3: Implementar el controlador**: `@RestController @RequestMapping("/api/v1/reportes/asistencia-estudiantes")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")`, Javadoc; `@RequestParam @DateTimeFormat(iso = DATE)
  LocalDate desde/hasta` obligatorios; usuario con `Authentication#getName()`. El CSV con `ResponseEntity<byte[]>`,
  `MediaType("text", "csv", UTF_8)` y `ContentDisposition.attachment().filename(...)`.
- [ ] **Step 4: Correr y verificar que pasan** — Expected: todas las pruebas de la clase en verde (14).
- [ ] **Step 5: Verificación del backend completa**

Run: `cd workflow-backend && sh ./mvnw spotless:apply && sh ./mvnw verify`
Expected: `BUILD SUCCESS` (Spotless, todas las pruebas, JaCoCo).

- [ ] **Step 6: Sin commit.**

---

### Task 5: Tipos y composable del frontend

**Files:**
- Create: `workflow-frontend/app/types/reporteAsistencia.ts`
- Create: `workflow-frontend/app/composables/useReporteAsistencia.ts`
- Test: `workflow-frontend/app/composables/useReporteAsistencia.spec.ts`

**Interfaces:**
- Consumes: contrato REST de la Tarea 4; `useApi().solicitar`; `useAutenticacionStore()` (`token`,
  `cerrarSesion`); `useRuntimeConfig().public.apiBase` (mismo patrón de descarga que `useLibretas.descargar`).
- Produces:
  - Tipos `TotalesAsistencia`, `FilaReporteAsistencia`, `ReporteAsistencia`, `SeccionReporteAsistencia`
    (campos idénticos a los records de la Tarea 2; `porcentajeInasistencia: number`) y
    `FiltrosReporteAsistencia { desde: string; hasta: string; gradoId: number | null; seccionId: number | null }`.
  - `crearFiltrosReporteAsistencia(hoy?: Date): FiltrosReporteAsistencia` — primer día del mes y hoy, en hora local, `yyyy-MM-dd`.
  - `rutaReporteAsistencia(filtros: FiltrosReporteAsistencia, csv?: boolean): string` — omite `gradoId`/`seccionId` nulos.
  - `seccionesDelGrado(secciones: SeccionReporteAsistencia[], gradoId: number | null): SeccionReporteAsistencia[]`
  - `gradosDisponibles(secciones: SeccionReporteAsistencia[]): { gradoId: number; grado: string }[]` (sin repetidos, en el orden recibido)
  - `useReporteAsistencia(): { listarSecciones(): Promise<SeccionReporteAsistencia[]>; consultar(f): Promise<ReporteAsistencia>; descargarCsv(f): Promise<{ archivo: Blob; nombre: string }> }`

- [ ] **Step 1: Escribir las pruebas** (mocks como `useLibretas.spec.ts`: `useApi`, store, `useRuntimeConfig`, `fetch`)

```ts
it('crea filtros del primer día del mes a hoy', ...)         // vi.setSystemTime(new Date(2026, 10, 17, 9)) → '2026-11-01'/'2026-11-17'; afterEach vi.useRealTimers()
it('arma la ruta con fechas y omite filtros vacíos', ...)     // '/api/v1/reportes/asistencia-estudiantes?desde=2026-11-01&hasta=2026-11-17'
it('incluye grado y sección y la variante csv', ...)          // '/api/v1/reportes/asistencia-estudiantes/csv?desde=…&hasta=…&gradoId=3&seccionId=7'
it('consulta el reporte y el catálogo de secciones', ...)     // solicitar llamado con las rutas exactas
it('descarga el csv con el nombre del Content-Disposition', ...)
it('usa un nombre por defecto si falta la cabecera', ...)     // 'reporte-asistencia_2026-11-01_2026-11-17.csv'
it('propaga el mensaje del JSON de error y cierra sesión en 401', ...)
it('filtra secciones por grado y lista grados sin repetir', ...)
```

- [ ] **Step 2: Correr y verificar que fallan** — `cd workflow-frontend && npx vitest run app/composables/useReporteAsistencia.spec.ts` → módulo no encontrado.
- [ ] **Step 3: Implementar tipos y composable.** `URLSearchParams` para la consulta; en la descarga, error si
  `content-type` no incluye `text/csv`.
- [ ] **Step 4: Correr y verificar que pasan** — Expected: 8 pruebas en verde.
- [ ] **Step 5: Sin commit.**

---

### Task 6: Componente, páginas, permisos y navegación

**Files:**
- Create: `workflow-frontend/app/components/ReporteAsistenciaAlumnos.vue`
- Create: `workflow-frontend/app/pages/reportes/asistencia.vue`
- Create: `workflow-frontend/app/pages/docente/reporte-asistencia.vue`
- Modify: `workflow-frontend/app/composables/usePermisosRol.ts` (`PERMISOS`, `PERMISOS_POR_ROL.DOCENTE`, `REGLAS_RUTA`)
- Modify: `workflow-frontend/app/composables/usePermisosRol.spec.ts` (si existe; si no, agregar el caso a `useReporteAsistencia.spec.ts`)
- Modify: `workflow-frontend/app/components/BarraLateralDocente.vue`, `workflow-frontend/app/pages/reportes/boletas.vue`

**Interfaces:**
- Consumes: todo lo producido por la Tarea 5.
- Produces: permiso `REPORTE_ASISTENCIA_VER` (módulo `Reportes`, nombre `Ver reporte de asistencia`,
  `sensible: true`); reglas `{ prefijo: '/reportes/asistencia', permiso: 'REPORTE_ASISTENCIA_VER' }` y
  `{ prefijo: '/docente/reporte-asistencia', permiso: 'REPORTE_ASISTENCIA_VER' }` **antes** de la regla `/docente`.

- [ ] **Step 1: Prueba de permisos**

```ts
it('administrador y docente acceden al reporte de asistencia; alumno y apoderado no', ...)
// puedeAcceder('ADMINISTRADOR', '/reportes/asistencia') === true
// puedeAcceder('DOCENTE', '/docente/reporte-asistencia') === true
// puedeAcceder('ALUMNO', '/reportes/asistencia') === false; puedeAcceder('APODERADO', '/docente/reporte-asistencia') === false
```

(Usar la firma real de `puedeAcceder` que ya prueban `useBoletasPension.spec.ts` / `useLibretas.spec.ts`.)

- [ ] **Step 2: Correr y verificar que falla** — `npx vitest run <archivo>` → `false` para el docente.
- [ ] **Step 3: Implementar permisos, componente y páginas**
  - Componente: `<script setup lang="ts">`; al montar `listarSecciones()` y genera el reporte con los filtros por
    defecto; selects de grado (`gradosDisponibles`) y sección (`seccionesDelGrado`; al cambiar el grado se limpia la
    sección si ya no pertenece); botón "Generar reporte"; tarjetas Presentes/Tardanzas/Inasistencias/Justificadas/
    Total; tabla Alumno, Grado, Sección, P, T, I, J, Total, % con clase `fila--riesgo` si
    `porcentajeInasistencia >= UMBRAL_RESALTADO`; paginación cliente (`TAMANIO_PAGINA = 20`, "Anterior/Siguiente",
    vuelve a la página 1 al generar); botón "Exportar CSV" que llama `descargarCsv` con los filtros **aplicados** y
    descarga con `URL.createObjectURL` + `<a download>` (como `ConsultaLibretas.descargarPdf`); estados de carga,
    vacío ("No hay asistencias registradas en el rango") y error. Estilos copiados de `pages/reportes/boletas.vue`.
  - `pages/reportes/asistencia.vue`: `definePageMeta({ middleware: 'autenticacion' })`, encabezado y pestañas
    `Cobranza` (`/reportes/boletas`) / `Asistencia` (`/reportes/asistencia`); las mismas pestañas en `boletas.vue`.
  - `pages/docente/reporte-asistencia.vue`: `definePageMeta({ layout: 'docente', middleware: 'autenticacion' })` + componente.
  - `BarraLateralDocente.vue`: `{ nombre: 'Reporte de asistencia', destino: '/docente/reporte-asistencia', icono: '/icons/menu-reportes.svg' }`
    después de "Asistencia de alumnos". (El menú "Reportes" del administrador ya apunta a `/reportes/boletas`; se
    llega a Asistencia por la pestaña.)
- [ ] **Step 4: Correr y verificar que pasa** — la prueba de permisos en verde.
- [ ] **Step 5: Verificación del frontend completa**

Run: `cd workflow-frontend && npm run format && npm run lint && npm run format:check && npm run typecheck && npm test`
Expected: sin errores de ESLint, Prettier ni vue-tsc; todas las pruebas de Vitest en verde.

- [ ] **Step 6: Prueba manual** (opcional si hay backend local): administrador y docente generan, paginan y exportan;
  el CSV abre en Excel con tildes.
- [ ] **Step 7: Sin commit.** Dejar `git status` de ambos repos para el resumen final.
