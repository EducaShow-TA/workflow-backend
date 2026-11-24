# Historial de boletas del estudiante Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Botón con el que el administrador completa las boletas faltantes de un alumno hasta el mes actual y ve todo su historial.

**Architecture:** `FinanzasService` completa boletas y mora de un alumno reutilizando la lógica del proceso diario; `HistorialBoletasService` lo invoca y arma la respuesta en una sola transacción. El frontend lo consume con un composable y una ventana modal en `/finanzas`.

**Tech Stack:** Java 25, Spring Boot 3.5.6, JUnit 5 + Mockito, H2 (`MODE=MySQL`); Nuxt 4, Vue 3, TypeScript, Vitest.

**Spec:** `workflow-backend/docs/superpowers/specs/2026-11-24-historial-boletas-estudiante-design.md`

## Global Constraints

- Rama `bryan-pisco` en ambos repos, rebasada sobre `origin/Development` antes de empezar. Sin push ni PR sin autorización expresa.
- Identificadores en español sin tildes ni ñ. Commits `tipo(alcance): descripción`, infinitivo, minúscula, sin punto final.
- Solo `ADMINISTRADOR`, con `@PreAuthorize` también en el servicio.
- Sin migraciones Flyway. Sin dependencias nuevas.
- No se emiten meses posteriores al actual ni fuera del cronograma. Boletas creadas: `MecanismoGeneracion.AUTOMATICO`.
- `procesarCicloDiario()` conserva su comportamiento; `FinanzasCicloDiarioSinTransaccionIntegrationTest` no se modifica.
- Logs SLF4J sin nombres ni DNI.
- Backend: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk` y `sh ./mvnw` (con JDK 27 Spotless falla). Frontend: npm ≥ 11.
- Código nuevo con el formato que deje `spotless:apply` / Prettier.

**Ajuste respecto a la spec §5:** la búsqueda por nombre o DNI ya existe en `useBusquedaAdministrador().buscarEstudiantes`; se reutiliza en vez de escribir otra en `useHistorialBoletas`.

## Review Focus

1. Alumno con compromiso cuyo primer mes es futuro: 200, 0 generadas, sin error → prueba en Task 1.
2. Alumno con matrículas de dos años: se completan ambos compromisos y los totales suman los dos → pruebas en Task 1 y Task 2.
3. Matrícula re-registrada (`EN_REGISTRO`) con compromiso inactivo: se muestran sus boletas y no se crean nuevas → prueba en Task 1.
4. Boleta de matrícula retirada, vencida y `PENDIENTE`: se muestra, no pasa a mora y no suma deuda → pruebas en Task 1 y Task 2.
5. Buscar con texto vacío o de un carácter: no se consulta al servidor → prueba en Task 3.

---

### Task 1: Completar boletas y mora de un estudiante

**Files:**
- Modify: `src/main/java/pe/edu/pucp/colegio/common/service/FinanzasService.java`
- Modify: `src/main/java/pe/edu/pucp/colegio/common/repository/CompromisoPagoRepository.java`
- Modify: `src/main/java/pe/edu/pucp/colegio/common/repository/BoletaPensionRepository.java`
- Test: `src/test/java/pe/edu/pucp/colegio/common/HistorialBoletasIntegrationTest.java` (crear)

**Interfaces:**
- Produces:
  - `FinanzasService.completarBoletasDeEstudiante(Integer estudianteId): BoletasCompletadas`, `@Transactional`
  - `public record BoletasCompletadas(YearMonth mesCorte, int generadas, int enMora)`, anidado en `FinanzasService`
  - `CompromisoPagoRepository.findAllByMatriculaEstudianteId(Integer): List<CompromisoPago>` (grafo `matricula`)
  - `BoletaPensionRepository.findAllByCompromisoMatriculaEstudianteIdAndActiveTrueOrderByMesCobroAsc(Integer): List<BoletaPension>` (grafo `compromiso`, `compromiso.matricula`)

- [ ] **Step 1: Crear la clase de prueba y sus datos**

`@SpringBootTest(properties = "app.jobs.catch-up-on-startup=false")`, `@AutoConfigureMockMvc`, `@Transactional`. Reloj `@Primary` fijo, como en `RiesgoApoderadoIntegrationTest`: `Clock.fixed(Instant.parse("2035-06-10T17:00:00Z"), ZoneId.of("America/Lima"))`.

Ayudas `persona`, `insertar` y datos por SQL iguales a `PagosApoderadoIntegrationTest`, con estas firmas: `int estudiante(String nombre)`, `int matricula(int estudiante, int periodo, String estado)`, `int compromiso(int matricula, String concepto, String mesInicio, int cuotas, boolean activo)` (monto `350 * cuotas`, día 15, primer vencimiento el 15 de `mesInicio`), `int boleta(int compromiso, String mes, String vencimiento, String estado)`, `int periodo(int anio)`. Cada alumno lleva apoderado principal vinculado. Insertar una cuenta `ADMINISTRADOR` y autenticarla con `SecurityContextHolder` con `UsernamePasswordAuthenticationToken(usuarioAdmin, null, ROLE_ADMINISTRADOR)` en `@BeforeEach` y `clearContext()` en `@AfterEach`.

- [ ] **Step 2: Escribir las pruebas que fallan**

Antes de leer con `jdbc`, llamar a `entityManager.flush()`.

```java
@Test
void completar_compromisoDesdeMarzo_generaHastaElMesActualYMarcaMora() {
  int c = compromiso(matricula(alumno, periodo(2035), "CONFIRMADA"), "Pensión 2035", "2035-03-01", 6, true);
  var r = finanzas.completarBoletasDeEstudiante(alumno);
  assertEquals(new FinanzasService.BoletasCompletadas(YearMonth.of(2035, 6), 4, 3), r);
  entityManager.flush();
  assertEquals(
      List.of("2035-03-01 EN_MORA", "2035-04-01 EN_MORA", "2035-05-01 EN_MORA", "2035-06-01 PENDIENTE"),
      mesesYEstados(c)); // consulta mes_cobro y estado_pago ordenados por mes_cobro
  assertEquals(4, jdbc.queryForObject(
      "select count(*) from auditoria_finanzas where compromiso_id=? and accion='BOLETA_GENERADA' and usuario=?",
      Integer.class, c, usuarioAdmin));
  assertEquals(3, jdbc.queryForObject(
      "select count(*) from auditoria_finanzas where compromiso_id=? and accion='BOLETA_EN_MORA' and usuario='SISTEMA'",
      Integer.class, c));
}
```

Las demás pruebas, con estos nombres y aserciones (`generadas`, `enMora`):

| Prueba | Datos | Esperado |
|---|---|---|
| `completar_segundaVez_noCambiaNada` | llamar dos veces | segunda: 0, 0; siguen 4 boletas |
| `completar_boletaPagada_noCambiaDeEstado` | marzo ya `PAGADA` | 3, 2; marzo sigue `PAGADA` |
| `completar_masMesesQueCuotas_respetaElCronograma` | desde marzo, 2 cuotas | 2, 2 |
| `completar_compromisoFuturo_noGenera` | desde `2035-08-01` | 0, 0 |
| `completar_dosAnios_completaAmbosCompromisos` | `Pensión 2034` desde `2034-11-01`, 2 cuotas; `Pensión 2035` desde marzo, 6 cuotas | 6, 5 |
| `completar_matriculaRetirada_noGeneraNiMarcaMora` | `RETIRADA`, compromiso activo, boleta de marzo `PENDIENTE` | 0, 0; sigue `PENDIENTE` |
| `completar_compromisoInactivo_noGenera` | `EN_REGISTRO`, compromiso inactivo, boleta de marzo `PAGADA` | 0, 0 |
| `completar_otroEstudiante_noLoToca` | segundo alumno con compromiso desde marzo | el segundo queda con 0 boletas |

- [ ] **Step 3: Verificar que fallan**

Run: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk sh ./mvnw test -Dtest=HistorialBoletasIntegrationTest`
Expected: error de compilación, `completarBoletasDeEstudiante` no existe.

- [ ] **Step 4: Implementar**

En `FinanzasService`:
- `private int generarBoletasPendientes(CompromisoPago, YearMonth)`: devuelve cuántas creó.
- `private boolean marcarSiVencida(BoletaPension, LocalDate hoy)`: cuerpo actual del bucle de mora (omite `RETIRADA`, marca y audita); `true` si cambió el estado. `procesarCicloDiario()` la usa.
- `completarBoletasDeEstudiante`: compromisos del alumno con `active` y matrícula distinta de `RETIRADA` → `generarBoletasPendientes`; luego todas sus boletas activas → `marcarSiVencida`. Javadoc: por qué no usa `transaccionPorCompromiso` (D8: todo o nada por alumno).

- [ ] **Step 5: Verificar que pasan, sin romper el ciclo diario**

Run: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk sh ./mvnw spotless:apply test -Dtest='HistorialBoletasIntegrationTest,FinanzasCicloDiarioSinTransaccionIntegrationTest,AuditoriaFinancieraIntegrationTest'`
Expected: `BUILD SUCCESS`, 0 fallos.

- [ ] **Step 6: Commit**

```bash
git add src && git commit -m "feat(finanzas): completar boletas faltantes de un estudiante (US-1.4, US-1.6)"
```

---

### Task 2: Endpoint del historial

**Files:**
- Create: `src/main/java/pe/edu/pucp/colegio/common/dto/HistorialBoletasRequest.java`
- Create: `src/main/java/pe/edu/pucp/colegio/common/dto/HistorialBoletasResponse.java`
- Create: `src/main/java/pe/edu/pucp/colegio/common/service/HistorialBoletasService.java`
- Modify: `src/main/java/pe/edu/pucp/colegio/common/controller/FinanzasController.java`
- Modify: `src/main/java/pe/edu/pucp/colegio/common/repository/ComprobantePagoRepository.java`
- Modify: `README.md` (tabla de rutas ~171 y sección "Pensiones y cobranza"), `postman/EducaShow-Sprint1.postman_collection.json`
- Test: `src/test/java/pe/edu/pucp/colegio/common/service/HistorialBoletasServiceTest.java` (crear), `HistorialBoletasIntegrationTest.java`

**Interfaces:**
- Consumes: todo lo producido en Task 1; `MatriculaRepository.findAllByEstudianteIdIn(List<Integer>)`; `EstudianteRepository.findById`.
- Produces: `POST /api/v1/boletas-pension/historiales` → 200 con `HistorialBoletasResponse`:

```java
public record HistorialBoletasRequest(@NotNull Integer estudianteId) {}

public record HistorialBoletasResponse(
    Integer estudianteId, String estudiante, YearMonth mesCorte, int boletasGeneradas,
    int boletasEnMora, Totales totales, List<MatriculaHistorial> matriculas) {
  public record Totales(BigDecimal pagado, BigDecimal pendiente, BigDecimal enMora) {}
  public record MatriculaHistorial(
      Integer matriculaId, Integer anio, Integer grado, String seccion, LocalDate fechaMatricula,
      EstadoMatricula estado, CompromisoHistorial compromiso, List<BoletaHistorial> boletas) {}
  public record CompromisoHistorial(
      Integer id, String concepto, Integer numeroCuotas, YearMonth mesInicio) {}
  public record BoletaHistorial(
      Integer id, YearMonth mesCobro, LocalDate fechaVencimiento, BigDecimal montoTotal,
      EstadoBoleta estadoPago, MecanismoGeneracion mecanismoGeneracion, String numeroComprobante) {}
}
```

- [ ] **Step 1: Escribir las pruebas unitarias que fallan**

`HistorialBoletasServiceTest` (`MockitoExtension`, entidades reales; `FinanzasService` y repositorios simulados):

| Prueba | Aserciones |
|---|---|
| `generar_estudianteInexistente_lanzaNotFound` | `ResponseStatusException` 404, mensaje `No existe el estudiante indicado`; `verifyNoInteractions(finanzas)` |
| `generar_sinMatriculas_devuelveListaVacia` | `matriculas` vacía; totales `0.00`; no consulta comprobantes |
| `generar_matriculaSinCompromiso_devuelveCompromisoNulo` | `compromiso == null`, `boletas` vacía |
| `generar_boletasDeTodosLosEstados_lasIncluyeYTotaliza` | tres boletas de 350 (`PAGADA`, `PENDIENTE`, `EN_MORA`); totales 350.00 cada uno; `numeroComprobante` solo en la pagada |
| `generar_matriculaRetirada_muestraBoletasSinSumarDeuda` | compromiso inactivo presente; una `PAGADA` y una `EN_MORA`: pagado 350.00, enMora 0.00 |
| `generar_variasMatriculas_ordenaDeMasRecienteAMasAntigua` | `anio` = `[2035, 2034]`; `pagado` suma ambas |
| `generar_matriculaInactivaNoRetirada_laOmite` | no aparece |

- [ ] **Step 2: Agregar las pruebas de integración que fallan**

```java
@Test
void historial_administrador_devuelveContratoCompleto() throws Exception {
  compromiso(matricula(alumno, periodo(2035), "CONFIRMADA"), "Pensión 2035", "2035-03-01", 6, true);
  mvc.perform(historial(alumno, "ADMINISTRADOR"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.mesCorte").value("2035-06"))
      .andExpect(jsonPath("$.boletasGeneradas").value(4))
      .andExpect(jsonPath("$.boletasEnMora").value(3))
      .andExpect(jsonPath("$.totales.pendiente").value(350.0))
      .andExpect(jsonPath("$.totales.enMora").value(1050.0))
      .andExpect(jsonPath("$.matriculas[0].anio").value(2035))
      .andExpect(jsonPath("$.matriculas[0].compromiso.mesInicio").value("2035-03"))
      .andExpect(jsonPath("$.matriculas[0].boletas[0].mesCobro").value("2035-03"))
      .andExpect(jsonPath("$.matriculas[0].boletas", hasSize(4)));
}
```

Además: `historial_apoderado_respondeForbidden` (403 y 0 boletas creadas), `historial_sinSesion_respondeUnauthorized` (401; limpiar el contexto de seguridad antes), `historial_sinEstudianteId_respondeBadRequest` (`{}` → 400), `historial_estudianteInexistente_respondeNotFound` (999999 → 404).

- [ ] **Step 3: Verificar que fallan**

Run: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk sh ./mvnw test -Dtest='HistorialBoletas*'`
Expected: error de compilación, `HistorialBoletasService` no existe.

- [ ] **Step 4: Implementar**

- `ComprobantePagoRepository.findAllByBoletaIdInAndActiveTrue(List<Integer>): List<ComprobantePago>`; se llama solo si hay boletas.
- `HistorialBoletasService.generar(Integer estudianteId): HistorialBoletasResponse`, `@Transactional`, `@PreAuthorize("hasRole('ADMINISTRADOR')")`, Javadoc en la clase. Primero `completarBoletasDeEstudiante`, después las lecturas. Matrículas con `active` o `RETIRADA`, orden `periodo.fechaInicio` descendente y luego `id` descendente; `anio` = año de `periodo.fechaInicio`. El compromiso se muestra aunque esté inactivo. Totales con escala 2; `pendiente` y `enMora` excluyen `RETIRADA`. Log `event=historial_boletas_generado estudianteId={} generadas={} enMora={}`.
- `FinanzasController.generarHistorial(@Valid @RequestBody HistorialBoletasRequest)` en `@PostMapping("/boletas-pension/historiales")`; inyectar el servicio por constructor.

- [ ] **Step 5: Documentar**

README: fila `POST /api/v1/boletas-pension/historiales` — "Completar y consultar el historial de boletas de un alumno", y un párrafo tras la boleta manual con el cuerpo `{ "estudianteId": 31 }` y las reglas D3, D4 y D5. Postman: petición "Generar historial de boletas del alumno (US-1.4 / US-1.5)" en la carpeta 04, después de "Rechazar boleta duplicada…", con la variable de estudiante que ya usa la carpeta.

- [ ] **Step 6: Verificar todo el backend**

Run: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk sh ./mvnw spotless:apply && JAVA_HOME=/usr/lib/jvm/java-25-openjdk sh ./mvnw clean verify && python3 -m json.tool postman/EducaShow-Sprint1.postman_collection.json > /dev/null`
Expected: `BUILD SUCCESS` (incluye `jacoco:check`) y JSON válido.

- [ ] **Step 7: Commit**

```bash
git add src README.md postman && git commit -m "feat(finanzas): generar historial de boletas del estudiante (US-1.4, US-1.5)"
```

---

### Task 3: Composable del historial (frontend)

**Files:**
- Modify: `app/types/boletaPension.ts`
- Create: `app/composables/useHistorialBoletas.ts`
- Test: `app/composables/useHistorialBoletas.spec.ts`

**Interfaces:**
- Consumes: contrato de Task 2; `useApi().solicitar`.
- Produces:
  - Tipos `HistorialBoletas`, `TotalesHistorial`, `MatriculaHistorial`, `CompromisoHistorial`, `BoletaHistorial`, espejo exacto de los records de Task 2 (`estado: 'EN_REGISTRO' | 'CONFIRMADA' | 'RETIRADA' | 'FINALIZADA'`, `compromiso: CompromisoHistorial | null`, `numeroComprobante: string | null`)
  - `useHistorialBoletas(): { generar(estudianteId: number): Promise<HistorialBoletas> }`
  - `describirHistorial(historial: Pick<HistorialBoletas, 'boletasGeneradas' | 'boletasEnMora' | 'matriculas'>): string`
  - `esBusquedaValida(texto: string): boolean` (2 o más caracteres sin espacios laterales)

- [ ] **Step 1: Escribir las pruebas que fallan** (mismo `vi.mock` de `useApi` que `useBoletasPension.spec.ts`)

```ts
await useHistorialBoletas().generar(4)
expect(solicitar).toHaveBeenCalledExactlyOnceWith('/api/v1/boletas-pension/historiales', {
  metodo: 'POST',
  cuerpo: { estudianteId: 4 },
})
```

`describirHistorial`, con una matrícula salvo en el último caso:

| generadas, enMora | Texto |
|---|---|
| 2, 1 | `Se generaron 2 boletas y 1 pasó a mora.` |
| 1, 2 | `Se generó 1 boleta y 2 pasaron a mora.` |
| 3, 0 | `Se generaron 3 boletas.` |
| 0, 2 | `2 boletas pasaron a mora.` |
| 0, 1 | `1 boleta pasó a mora.` |
| 0, 0 | `No había boletas por generar ni por pasar a mora.` |
| 0, 0 sin matrículas | `El alumno no tiene matrículas registradas.` |

`esBusquedaValida`: `''`, `'  '` y `' a '` → `false`; `'An'` y `'12345678'` → `true`.

- [ ] **Step 2: Verificar que fallan** — Run: `npm test -- useHistorialBoletas`. Expected: FAIL, no existe el módulo.

- [ ] **Step 3: Implementar** los tipos y el composable con las firmas indicadas.

- [ ] **Step 4: Verificar** — Run: `npm test -- useHistorialBoletas`. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app && git commit -m "feat(finanzas): agregar consulta del historial de boletas (US-1.4, US-1.5)"
```

---

### Task 4: Botón y ventana del historial

**Files:**
- Create: `app/components/ModalHistorialBoletas.vue`
- Modify: `app/pages/finanzas/index.vue`

**Interfaces:**
- Consumes: Task 3; `useBusquedaAdministrador().buscarEstudiantes(texto): Promise<EstudianteEncontrado[]>`; `etiquetaEstadoBoleta`, `nombreMes`, `formatearFecha`, `obtenerMensajeError`; `VentanaModal` (`ancho="amplia"`, `bloqueada`), `AvisoMensaje`, `BotonAccion`.
- Produces: `ModalHistorialBoletas` con prop `abierta: boolean` y eventos `cerrar: []`, `actualizada: []`.

- [ ] **Step 1: Crear `ModalHistorialBoletas.vue`**

Título "Historial de boletas"; descripción "Completa las boletas que falten hasta el mes actual según el compromiso de pago y muestra todas las boletas del alumno."

- Formulario: campo "Nombre o DNI del alumno" y botón "Buscar", deshabilitado si `!esBusquedaValida(texto)` o mientras busca. Resultados como opciones de radio (`nombreCompleto · dni · aula`). Sin resultados: "No se encontraron alumnos."
- Pie: "Cerrar" y "Generar historial" (deshabilitado sin alumno elegido o mientras procesa; texto "Generando…"). `bloqueada` mientras procesa.
- Al abrir, al buscar o al elegir otro alumno se borran el historial y el error anteriores, para no mostrar datos de un alumno bajo el nombre de otro. Se ignora una búsqueda que termine tras cerrar o tras otra búsqueda (contador como `ultimaSolicitud` en `finanzas/index.vue`).
- Resultado: `AvisoMensaje` con `describirHistorial` (`exito`, o `informacion` si no hubo cambios); totales Pagado, Pendiente y En mora; una tabla por matrícula con encabezado `{anio} · {grado}.º {seccion}`, etiqueta "Retirada" si corresponde, y columnas Boleta, Mes, Vencimiento, Estado, Comprobante (`—` si es nulo), Total. Sin compromiso: "Sin compromiso de pago". Con compromiso y sin boletas: "Aún no hay boletas emitidas".
- Emite `actualizada` si `boletasGeneradas + boletasEnMora > 0`.

- [ ] **Step 2: Integrar en `finanzas/index.vue`**

Botón secundario "Generar historial" junto a "+ Generar boleta manual", agrupados en un contenedor `flex` con `gap: 12px` y salto de línea. `@actualizada` → `Promise.all([cargarCatalogos(), consultar()])`; la ventana permanece abierta.

- [ ] **Step 3: Verificar formato, tipos y pruebas**

Run: `npx prettier --write app/components/ModalHistorialBoletas.vue app/pages/finanzas/index.vue && npm run lint && npm run format:check && npm run typecheck && npm test`
Expected: todo en verde.

- [ ] **Step 4: Probar a mano** (backend y frontend locales; si el entorno no lo permite, reportarlo como no verificado)

1. Alumno con compromiso al día → "No había boletas por generar ni por pasar a mora." y sus boletas.
2. Con el servidor encendido, borrar por SQL una boleta impaga de un alumno **local** de prueba → "Se generó 1 boleta…" y el listado de fondo se actualiza.
3. Repetir → no se duplica.
4. Alumno sin matrícula → "El alumno no tiene matrículas registradas."
5. Elegir otro alumno → desaparece el historial anterior. Revisar también a 400 px de ancho.

- [ ] **Step 5: Commit**

```bash
git add app && git commit -m "feat(finanzas): agregar botón del historial de boletas (US-1.4, US-1.5)"
```

---

### Cierre

- [ ] Commitear spec y plan en el backend: `docs(finanzas): agregar spec y plan del historial de boletas`.
- [ ] Verificar `git log --merges origin/Development..HEAD` vacío en ambos repos.
- [ ] Pedir autorización antes de `push` y de abrir los PR (primero el backend). Títulos: los de la spec §8.
