# Reporte de inasistencias y tardanzas de alumnos (US-2.6)

- **Fecha:** 2026-11-17
- **Historia:** US-2.6 — Como administrador o docente, quiero generar reportes detallados de inasistencias y
  tardanzas de los alumnos para identificar patrones e intervenir a tiempo.
- **Criterios de aceptación (Product Backlog):**
  1. Permite filtrar por rango de fechas, grado y sección.
  2. Muestra y exporta el consolidado con totales de asistencias, tardanzas e inasistencias.
- **Repositorios:** `workflow-backend` y `workflow-frontend`, rama `bryan-pisco`.

## 1. Decisiones tomadas

| # | Tema | Decisión |
|---|---|---|
| D1 | Unidad de conteo | **Sesiones de clase.** Cada registro de `asistencias_estudiante` cuenta 1. No se infieren días. |
| D2 | Alcance del docente | Igual que el historial (`AccesoAsistenciaService`): ve las sesiones que dicta **y** todas las sesiones de las secciones donde tiene clase vigente en la fecha de la sesión. |
| D3 | Exportación | **CSV generado en el backend** con `commons-csv`, misma consulta y mismo control de acceso que la vista. |
| D4 | Justificadas | Columna aparte. El **% de inasistencia cuenta solo `AUSENTE`** (no justificadas). |
| D5 | Detalle | **Consolidado por alumno**, ordenado por % de inasistencia descendente. Sin desglose por área ni por día. |
| D6 | Paginación | **En el cliente** (20 filas). El servidor devuelve todas las filas: el `GROUP BY` las acota al número de alumnos. |
| D7 | Persistencia | Consulta JPQL agregada en el repositorio. **Sin migraciones Flyway.** |
| D8 | Catálogo de filtros | Endpoint propio del reporte con las secciones filtrables, porque `SeccionResponse` y `MiClaseResponse` no traen `gradoId` y no hay endpoint de grados. No se modifican DTOs de otros módulos. |

Fuera de alcance: conteo por días, desglose por área o día de la semana, exportación PDF, pantalla de
historial de un alumno para administrador o docente, umbral configurable.

## 2. Reglas de conteo

- Entran los registros con `asistencias_estudiante.active = true` cuya sesión tiene `sesiones_clase.active = true`,
  `sesiones_clase.cancelada = false` (V31: días no lectivos o cambios de horario) y `desde <= sesiones_clase.fecha <= hasta` (ambos inclusive).
- `PENDIENTE_MARCAJE` **no cuenta** en ningún total.
- Columnas: `presentes` (`PRESENTE`), `tardanzas` (`TARDANZA`), `inasistencias` (`AUSENTE`),
  `justificadas` (`JUSTIFICADA`), `totalSesiones` = suma de las cuatro.
- `porcentajeInasistencia` = `inasistencias * 100 / totalSesiones`, `BigDecimal` con 1 decimal y `HALF_UP`;
  0 si `totalSesiones = 0`.
- Agrupación por **(estudiante, sección de la sesión)**: si un alumno cambió de sección dentro del rango, aparece
  una fila por sección.
- Los alumnos sin registros contables en el rango no aparecen.
- Orden: `porcentajeInasistencia` desc, `inasistencias` desc, `nombreCompleto` asc.
- `totales` = suma de las filas devueltas (después del filtro de acceso).

## 3. Contrato REST

Todas las rutas requieren `ADMINISTRADOR` o `DOCENTE` (`@PreAuthorize`), además del control en el servicio.

### 3.1 `GET /api/v1/reportes/asistencia-estudiantes`

| Parámetro | Tipo | Obligatorio |
|---|---|---|
| `desde` | `yyyy-MM-dd` | sí |
| `hasta` | `yyyy-MM-dd` | sí |
| `gradoId` | entero | no |
| `seccionId` | entero | no |

`gradoId` y `seccionId` se combinan con AND: una sección de otro grado produce una lista vacía.

```json
{
  "desde": "2026-11-01",
  "hasta": "2026-11-17",
  "totales": {"presentes": 40, "tardanzas": 5, "inasistencias": 6, "justificadas": 1, "totalSesiones": 52},
  "alumnos": [
    {
      "estudianteId": 12,
      "nombreCompleto": "Ana Quispe Rojas",
      "gradoId": 3,
      "grado": "3.° Secundaria",
      "seccionId": 7,
      "seccion": "A",
      "presentes": 40,
      "tardanzas": 5,
      "inasistencias": 6,
      "justificadas": 1,
      "totalSesiones": 52,
      "porcentajeInasistencia": 11.5
    }
  ]
}
```

### 3.2 `GET /api/v1/reportes/asistencia-estudiantes/csv`

Mismos parámetros y mismas reglas. Responde `200` con `Content-Type: text/csv;charset=UTF-8`,
`Content-Disposition: attachment; filename="reporte-asistencia_{desde}_{hasta}.csv"` y BOM UTF-8 al inicio
(para que Excel muestre las tildes). Columnas, en este orden:

`Alumno,Grado,Seccion,Presentes,Tardanzas,Inasistencias,Justificadas,Total sesiones,% inasistencia`

Una fila por alumno, en el mismo orden que el JSON (los textos que empiezan con `=`, `+`, `-`, `@`, tabulador o retorno se
anteponen con `'` para evitar inyección de fórmulas), y una última fila `TOTAL` con los totales y el % global
(`inasistencias * 100 / totalSesiones` de los totales). Los valores de texto se escapan con
`CSVFormat.DEFAULT` (comillas cuando hace falta).

### 3.3 `GET /api/v1/reportes/asistencia-estudiantes/secciones`

Catálogo para los filtros. Administrador: todas las secciones activas. Docente: las secciones activas
donde tiene al menos una `SesionHorario` de un `HorarioSeccion` activo o dicta alguna `SesionClase` activa
(la misma regla que D2).

```json
[{"seccionId": 7, "seccion": "A", "gradoId": 3, "grado": "3.° Secundaria"}]
```

Orden: nivel educativo (INICIAL, PRIMARIA, SECUNDARIA), número de grado y código de sección.

### 3.4 Errores

Todos con el JSON estándar de `GlobalExceptionHandler`.

| Caso | Código |
|---|---|
| Falta `desde` o `hasta`, o formato inválido | 400 |
| `desde > hasta` | 422 (`BusinessException`, igual que el rango invertido del reporte de Finanzas) |
| Docente con `seccionId` donde, dentro del rango, no tiene clase vigente (`docenteTieneClaseEnSeccion`) ni dicta ninguna sesión | 403 ("No tiene clases en esta sección en el rango de fechas seleccionado") |
| Rol `ALUMNO` o `APODERADO` | 403 |
| Sin token | 401 |

`GlobalExceptionHandler` ya traduce `MissingServletRequestParameterException` y
`MethodArgumentTypeMismatchException` a 400 con el JSON estándar: no se agregan handlers.

## 4. Backend

Paquete `pe.edu.pucp.colegio.common`, como el resto del código de asistencia.

| Archivo | Responsabilidad |
|---|---|
| `repository/AsistenciaEstudianteRepository` (+ método) | `consolidarPorAlumno(desde, hasta, gradoId, seccionId, personaIdDocente)`: JPQL con `GROUP BY` estudiante y sección, `SUM(CASE WHEN … THEN 1 ELSE 0 END)` por estado. Devuelve una projection `ConsolidadoAsistenciaFila`. |
| `repository/ConsolidadoAsistenciaFila` | Interfaz projection: ids, nombre, apellidos, grado (número y nivel), sección y los cuatro conteos. |
| `repository/SesionClaseRepository` (+ método) o `SeccionRepository` | Secciones filtrables del docente para §3.3. |
| `service/ReporteAsistenciaService` | Javadoc + SLF4J. Valida el rango, resuelve la cuenta y el rol, comprueba la sección del docente (sin `docenteTieneClaseEnSeccion` ni sesiones dictadas en el rango → 403), calcula porcentajes y totales, ordena, arma el catálogo de secciones. |
| `service/ReporteAsistenciaCsv` | `@Component` que convierte un `ReporteAsistenciaResponse` en `byte[]` CSV (BOM + `commons-csv`). Sin lógica de negocio ni de acceso. |
| `controller/ReporteAsistenciaController` | Javadoc. Tres rutas de §3; solo delega. |
| `dto/ReporteAsistenciaResponse`, `TotalesAsistenciaResponse`, `FilaReporteAsistenciaResponse`, `SeccionReporteAsistenciaResponse` | `record`. Sufijo `*Response` como el código vecino. |

**Filtro del docente dentro de la consulta** (`personaIdDocente` es `null` para el administrador):

```
and (:personaId is null
     or s.docente.persona.id = :personaId
     or exists (select 1 from SesionHorario sh
                where sh.docente.persona.id = :personaId
                  and sh.horario.seccion = s.seccion
                  and sh.horario.active = true
                  and sh.vigenteDesde <= s.fecha
                  and (sh.vigenteHasta is null or sh.vigenteHasta >= s.fecha)))
```

Es la regla de `AccesoAsistenciaService.puedeConsultarSesion` aplicada sesión por sesión, así que el reporte
nunca muestra una sesión que el historial le negaría al mismo docente.

**Etiqueta del grado:** `"{numero}.° {Nivel}"` con el nivel en formato título (`Inicial`, `Primaria`,
`Secundaria`), armada en el servicio.

## 5. Frontend

| Archivo | Responsabilidad |
|---|---|
| `app/types/reporteAsistencia.ts` | `ReporteAsistencia`, `TotalesAsistencia`, `FilaReporteAsistencia`, `SeccionReporteAsistencia`, `FiltrosReporteAsistencia`. |
| `app/composables/useReporteAsistencia.ts` | `listarSecciones()`, `consultar(filtros)`, `descargarCsv(filtros)` (Blob + descarga con el nombre del `Content-Disposition` o uno por defecto). Usa `useApi`. Omite los parámetros vacíos. |
| `app/composables/useReporteAsistencia.spec.ts` | Pruebas del composable. |
| `app/components/ReporteAsistenciaAlumnos.vue` | Filtros (fechas, grado, sección dependiente del grado), tarjetas de totales, tabla con paginación en el cliente (20 filas), filas resaltadas si `porcentajeInasistencia >= 30` (constante `UMBRAL_RESALTADO`), botón "Exportar CSV", estados de carga, vacío y error. Sin llamadas HTTP directas. |
| `app/pages/reportes/asistencia.vue` | Página del administrador; navegación entre "Cobranza" (`/reportes/boletas`) y "Asistencia". |
| `app/pages/docente/reporte-asistencia.vue` | Página del docente con el mismo componente. |
| `app/composables/usePermisosRol.ts` | Permiso `REPORTE_ASISTENCIA_VER` (módulo Reportes) para ADMINISTRADOR y DOCENTE; reglas de ruta `/reportes/asistencia` y `/docente/reporte-asistencia`. |
| `BarraLateralAdministrador.vue`, `BarraLateralDocente.vue` | Enlaces a las páginas nuevas. |

Fechas por defecto: desde el primer día del mes actual hasta hoy, calculadas con la fecha local del
navegador en formato `yyyy-MM-dd`. Son solo un valor inicial del filtro, no una validación de negocio.

## 6. Pruebas

**Backend unitarias** (`ReporteAsistenciaServiceTest`, `ReporteAsistenciaCsvTest`; Mockito, repositorios mockeados):

- `generar_rangoInvertido_lanzaBusinessException`
- `generar_administrador_noFiltraPorDocente`
- `generar_docenteConSeccionAjena_lanza403`
- `generar_docenteSinSeccion_filtraPorSuPersona`
- `generar_sinSesionesContables_porcentajeCero`
- `generar_variosAlumnos_ordenaPorPorcentajeDescYNombre`
- `generar_variasFilas_sumaTotales`
- `exportar_reporte_incluyeBomEncabezadoFilasYTotal`
- `exportar_nombreConComa_loEscapa`

**Backend integración** (`ReporteAsistenciaIntegrationTest`, `@SpringBootTest` sobre H2):

- La consulta real excluye `PENDIENTE_MARCAJE`, registros y sesiones inactivos y fechas fuera de rango.
- Un docente no ve sesiones de secciones donde no tiene clase; sí ve las de otra área en su sección.
- Filtro por `gradoId` y por `seccionId`.
- `GET …/csv` responde 200 con `text/csv`; `ALUMNO` recibe 403; sin `desde` responde 400; rango invertido 422.

**Frontend** (`useReporteAsistencia.spec.ts`, Vitest):

- `consultar` arma la URL con los parámetros y omite los vacíos.
- `descargarCsv` pide un Blob y usa el nombre del `Content-Disposition`.
- Propaga el mensaje de error del JSON estándar.
- Las fechas por defecto se prueban con `vi.useFakeTimers()` + `vi.setSystemTime()` (sin fechas literales que caduquen).

**Verificación final:** `sh ./mvnw verify` (JDK 25) en el backend; `npm run lint`, `npm run format:check`,
`npm run typecheck` y `npm test` en el frontend.

## 7. Entrega

Sin commits ni push por indicación del integrante: los cambios quedan en el árbol de trabajo de `bryan-pisco`
en ambos repositorios. Commits sugeridos al integrarlo:

- backend: `feat(asistencia): agregar reporte de inasistencias y tardanzas de alumnos`
- frontend: `feat(asistencia): agregar pantalla de reporte de inasistencias y tardanzas`
