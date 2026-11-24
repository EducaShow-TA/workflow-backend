# Historial de boletas del estudiante (US-1.4, US-1.5)

- **Fecha:** 2026-11-24
- **Pendiente:** "Proceso/botón para generar boletas mensuales y poder simular meses en la exposición."
- **Precisión del solicitante:** el administrador genera el historial de boletas de un alumno, de toda su historia
  académica, desde que se matriculó hasta la fecha actual, y ve todas sus boletas sin importar su estado.
- **Historias:** US-1.4 (generación), US-1.5 (consulta y estados) y US-1.6 (mora).
- **Repositorios:** `workflow-backend` y `workflow-frontend`, rama `bryan-pisco`.

## 1. Objetivo

Con un botón, el administrador elige un alumno; el sistema completa las boletas que le falten hasta el mes actual y
muestra todas sus boletas (pagadas, pendientes y en mora) de todas sus matrículas.

**Éxito:** el historial coincide con las boletas guardadas, repetir la acción no duplica boletas y no se cobra ningún mes
fuera del compromiso acordado.

**Situación actual:** el listado de `/finanzas` filtra por estudiante, pero solo ofrece alumnos que ya tienen boletas,
oculta las de matrículas retiradas y no agrupa ni totaliza. El proceso diario completa boletas, pero no se puede lanzar
para un alumno.

## 2. Decisiones tomadas

Confirmadas por el solicitante: D1, D2 y D3. Las demás son propuestas de este diseño.

| # | Tema | Decisión |
|---|---|---|
| D1 | Alcance | **Un alumno a la vez**, todas sus matrículas, hasta el **mes actual** del servidor. No se emiten meses futuros. |
| D2 | Acción | Crea las boletas faltantes **y** muestra todas, sin importar su estado. |
| D3 | Cronograma | **Se respeta el compromiso**: solo se crean meses de su cronograma. Una matrícula sin compromiso activo no genera boletas. No se inventan montos ni vencimientos. |
| D4 | Mora | Las boletas `PENDIENTE` del alumno con vencimiento anterior a hoy pasan a `EN_MORA` (regla actual, sin recargo). |
| D5 | Retiradas | Se muestran las boletas de matrículas `RETIRADA`, pero no se les crean boletas ni se les cambia el estado. |
| D6 | Mecanismo | Las boletas creadas quedan como `AUTOMATICO`: siguen el cronograma. |
| D7 | Auditoría | Emisión a nombre del administrador; mora como `SISTEMA` (regla existente). |
| D8 | Transacción | Una por alumno: se completa todo o no se guarda nada. |
| D9 | Persistencia | **Sin migraciones Flyway.** |

Fuera de alcance: meses futuros, generación para todos los alumnos a la vez, exportación PDF/CSV, recargo monetario por
mora, cambio de la fecha del sistema y carga de datos de demostración (ver §7).

## 3. Contrato REST

### `POST /api/v1/boletas-pension/historiales`

Solo `ADMINISTRADOR`. Es POST porque puede crear boletas; es idempotente en la práctica. Cuerpo:
`{ "estudianteId": 4 }`. Respuesta `200 OK`:

```json
{
  "estudianteId": 4,
  "estudiante": "Ana Pérez Díaz",
  "mesCorte": "2026-11",
  "boletasGeneradas": 2,
  "boletasEnMora": 1,
  "totales": { "pagado": 500.00, "pendiente": 500.00, "enMora": 500.00 },
  "matriculas": [
    {
      "matriculaId": 8,
      "anio": 2026,
      "grado": 1,
      "seccion": "A",
      "fechaMatricula": "2026-09-01",
      "estado": "CONFIRMADA",
      "compromiso": { "id": 5, "concepto": "Pensión 2026", "numeroCuotas": 4, "mesInicio": "2026-09" },
      "boletas": [
        {
          "id": 17,
          "mesCobro": "2026-09",
          "fechaVencimiento": "2026-09-20",
          "montoTotal": 500.00,
          "estadoPago": "PAGADA",
          "mecanismoGeneracion": "AUTOMATICO",
          "numeroComprobante": "PEN-1A2B3C4D"
        }
      ]
    }
  ]
}
```

- `boletasGeneradas` y `boletasEnMora` cuentan solo lo hecho en esta llamada.
- `matriculas` incluye las activas y las retiradas, de la más reciente a la más antigua; `boletas`, por `mesCobro`
  ascendente.
- `compromiso` es `null` si la matrícula nunca tuvo uno; si lo tuvo, se muestra aunque esté inactivo.
- `totales` suma las boletas mostradas. **Excluye de `pendiente` y `enMora` las de matrículas retiradas**, igual que el
  reporte de cobranza; lo pagado sí cuenta.
- `numeroComprobante` es `null` si la boleta no está pagada.

| Caso | Código |
|---|---|
| `estudianteId` ausente | 400 |
| Sin sesión / otro rol | 401 / 403 |
| El estudiante no existe | 404 |
| Choque simultáneo con otra generación (`uk_boleta_compromiso_mes`) | 409; reintentar es seguro |
| Estudiante sin matrículas | 200 con `matriculas: []` |

Los errores usan el JSON estándar de `GlobalExceptionHandler`.

## 4. Backend

| Unidad | Responsabilidad |
|---|---|
| `HistorialBoletasService` (nuevo) | Valida el estudiante, pide completar sus boletas y arma la respuesta. `@Transactional` y `@PreAuthorize("hasRole('ADMINISTRADOR')")`. |
| `FinanzasService.completarBoletasDeEstudiante(Integer)` (nuevo método) | Recorre sus compromisos activos con matrícula no retirada, llama a `generarBoletasPendientes` con el mes actual y marca la mora. Devuelve los dos contadores. |
| `FinanzasService.generarBoletasPendientes` | Pasa a devolver cuántas creó. La marca de mora se extrae a un método compartido. `procesarCicloDiario()` no cambia su comportamiento. |
| `FinanzasController` | Endpoint nuevo. |
| `BoletaPensionRepository`, `CompromisoPagoRepository`, `ComprobantePagoRepository`, `MatriculaRepository` | Consultas por estudiante, en bloque para evitar N+1. |
| DTOs (nuevos) | `HistorialBoletasRequest`, `HistorialBoletasResponse`. |

- Se conserva la regla actual de no repetir boleta por estudiante, concepto y mes.
- Log SLF4J sin nombres ni DNI: `event=historial_boletas_generado estudianteId=… generadas=… enMora=…`.
- Se actualizan `README.md` y la colección de Postman.

## 5. Frontend

| Unidad | Responsabilidad |
|---|---|
| `app/types/boletaPension.ts` | `HistorialBoletas`, `MatriculaHistorial`, `BoletaHistorial`. |
| `app/composables/useHistorialBoletas.ts` (nuevo) | `generar(estudianteId)`, `describirHistorial(historial)` y `esBusquedaValida(texto)`. La búsqueda por nombre o DNI reutiliza `useBusquedaAdministrador().buscarEstudiantes`. |
| `app/components/ModalHistorialBoletas.vue` (nuevo) | Sobre `VentanaModal`: buscador, botón "Generar historial", resumen, totales y una tabla por matrícula. Bloqueado mientras procesa. |
| `app/pages/finanzas/index.vue` | Botón "Generar historial" junto a "Generar boleta manual". Si se crearon boletas o cambió alguna a mora, recarga opciones y listado. |

- Permiso: `/finanzas` ya exige `BOLETA_GESTIONAR`.
- El resumen indica lo hecho: "Se generaron 2 boletas y 1 pasó a mora" o "No había boletas por generar ni por pasar a mora".
- Matrícula sin compromiso: "Sin compromiso de pago". Retirada: etiqueta "Retirada". Sin matrículas: "El alumno no tiene
  matrículas registradas".
- El historial es solo de consulta; el pago se sigue registrando desde el listado.

## 6. Pruebas

**Backend, unitarias** (`HistorialBoletasServiceTest`, Mockito, repositorios simulados):

- `generar_estudianteInexistente_lanzaNotFound`
- `generar_matriculaSinCompromiso_devuelveCompromisoNulo`
- `generar_boletasDeTodosLosEstados_lasIncluyeYTotaliza`
- `generar_matriculaRetirada_muestraBoletasSinSumarDeuda`
- `generar_variasMatriculas_ordenaDeMasRecienteAMasAntigua`

**Backend, integración** (`@SpringBootTest`, `@Transactional`, fechas relativas al `Clock`):

- Completa las boletas desde el inicio del compromiso hasta el mes actual y ninguna posterior.
- Las vencidas quedan `EN_MORA`; una `PAGADA` no cambia.
- Segunda llamada: 0 generadas y 0 en mora, mismo historial.
- No supera el número de cuotas y no toca boletas de otro alumno.
- La auditoría de emisión registra al administrador. `APODERADO` recibe 403.

`FinanzasCicloDiarioSinTransaccionIntegrationTest` debe seguir pasando sin cambios.

**Frontend** (`useHistorialBoletas.spec.ts`): validación del texto de búsqueda, ruta y cuerpo de `generar`, y
`describirHistorial` con cambios, sin cambios y en singular.

## 7. Uso en la exposición

La aplicación no permite registrar un primer vencimiento pasado, así que un alumno matriculado este mes tendrá una sola
boleta. Para mostrar varios meses hay que preparar alumnos de demostración con compromisos que empiecen antes (script SQL
aparte, solo en el entorno de demostración y con snapshot previo de la RDS). Ese script no forma parte de esta spec.

El proceso que corre al arrancar el servidor también completa boletas. Por eso, si los datos se cargan antes de reiniciar,
el botón mostrará "No había boletas por generar ni por pasar a mora". Para mostrar la creación en vivo, cargar los datos con el servidor
ya encendido.

## 8. Integración

Dos PR hacia `Development`, primero el del backend:

- Backend: `feat(finanzas): generar historial de boletas del estudiante (US-1.4, US-1.5)`
- Frontend: `feat(finanzas): agregar historial de boletas del estudiante (US-1.4, US-1.5)`
