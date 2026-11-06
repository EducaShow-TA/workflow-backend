# Tareas y evidencias de seguimiento académico

El módulo usa `periodoId` para identificar el grado y la sección, y `competenciaCodigo` para identificar el curso o ámbito curricular disponible en el modelo actual.

## Tareas

- `POST /api/v1/tareas`: crea una tarea y genera su lista de seguimiento con las matrículas confirmadas del período.
- `GET /api/v1/tareas?periodoId={id}`: lista las tareas del período con alumno, matrícula y estado de cumplimiento.
- `PATCH /api/v1/tareas/{tareaId}/entregas/{matriculaId}`: actualiza si la entrega fue cumplida. Enviar `{"cumplida": true}` para marcarla y `false` para revertirla.

Ejemplo de creación:

```json
{
  "periodoId": 12,
  "competenciaCodigo": "MAT-RESUELVE",
  "titulo": "Práctica de fracciones",
  "descripcion": "Resolver los ejercicios 1 al 10.",
  "fechaLimite": "2026-11-20T23:59:00",
  "ponderacion": 20.00
}
```

La ponderación es opcional y, si se informa, admite valores de 0 a 100.

## Calificaciones y evidencias

- `POST /api/v1/calificaciones` registra o actualiza la calificación de una matrícula en una competencia. La solicitud es `multipart/form-data` con la parte JSON `datos` y, opcionalmente, la parte `archivo`.
- La parte `datos` requiere `periodoId`, `matriculaId`, `competenciaCodigo`, `nota` (`AD`, `A`, `B` o `C`) y `evidenciaUrl` cuando no se envía archivo. Debe enviarse un archivo o una URL HTTP/HTTPS.
- `GET /api/v1/seguimiento/competencias/{codigo}?periodoId={id}` devuelve las calificaciones y los metadatos/enlace de evidencia para la ficha de seguimiento. Las notas calculadas incluyen `consolidada: true` y `puntajeConsolidado`.
- El registro manual en `/api/v1/calificaciones` se conserva para competencias/períodos sin evaluaciones parciales configuradas. Cuando existen, el resultado debe ingresarse en la evaluación parcial correspondiente.
- Los archivos se descargan mediante `GET /api/v1/evidencias/{calificacionId}/archivo`. El límite por archivo es 10 MB.
- El docente obtiene sus clases con `GET /api/v1/docentes/mis-clases`: una fila por sección y área que dicta (según sus sesiones de clase), con el periodo vigente de la sección, el total de alumnos y las competencias activas del área.
- `GET /api/v1/docentes/mis-clases/{periodoId}/alumnos` devuelve los alumnos con matrícula confirmada en ese periodo (`matriculaId`, `estudianteId`, `dni`, `nombreCompleto`). Responde `403` si el docente no dicta en esa sección.

El directorio de almacenamiento puede configurarse con la propiedad `app.storage.evidence-dir`; el valor predeterminado es `uploads/evidencias`. El acceso a estas rutas está restringido a los roles docente y administrador.

## Evaluaciones parciales y consolidación del logro

Las evaluaciones parciales son independientes de las tareas. Cada evaluación se asocia a un período y una competencia, y tiene un peso relativo entre 0 y 100. El administrador las crea, edita y desactiva; un docente solo puede consultar y calificar competencias dictadas en sus sesiones.

- `GET /api/v1/competencias/{codigo}/regla-consolidacion` consulta los umbrales (solo ADMINISTRADOR). Si aún no se configuraron, devuelve los valores predeterminados.
- `PUT /api/v1/competencias/{codigo}/regla-consolidacion` configura los umbrales `umbralB`, `umbralA` y `umbralAd`. Deben cumplir `1 < umbralB < umbralA < umbralAd <= 4`.
- `POST /api/v1/evaluaciones-parciales` crea una evaluación (solo ADMINISTRADOR). Enviar `periodoId`, `competenciaCodigo`, `titulo`, `ponderacion` y, opcionalmente, `descripcion`.
- `GET /api/v1/evaluaciones-parciales?periodoId={id}&competenciaCodigo={codigo}` lista las evaluaciones para la competencia.
- `PUT /api/v1/evaluaciones-parciales/{evaluacionId}` actualiza título, descripción y peso; `DELETE` desactiva la evaluación. Cambiar o desactivar una evaluación recalcula los logros abiertos.
- `GET /api/v1/evaluaciones-parciales/{evaluacionId}/calificaciones` devuelve la lista de matrículas confirmadas, incluidas las que aún no tienen nota.
- `PUT /api/v1/evaluaciones-parciales/{evaluacionId}/matriculas/{matriculaId}` registra o corrige la nota parcial como JSON o `multipart/form-data` (parte `datos` y archivo opcional `archivo`). El cuerpo admite `nota` (`AD`, `A`, `B`, `C`), `justificada` y `evidenciaUrl` opcional. Si está justificada, `nota` debe ser `null`; si no, la nota es obligatoria. La evidencia URL debe usar HTTP o HTTPS; archivo y URL son excluyentes.
- Los archivos parciales se descargan mediante `GET /api/v1/evaluaciones-parciales/calificaciones/{calificacionId}/archivo`.
- `POST /api/v1/logros/recalculo` fuerza el recálculo administrativo; el cuerpo debe contener exactamente uno de `periodoId`, `gradoId` o `seccionId`.
- `POST /api/v1/periodos/{periodoId}/cierre-actas` cierra las actas (solo ADMINISTRADOR), registra usuario y fecha y bloquea cambios posteriores.
- `GET /api/v1/periodos/{periodoId}/cierre-actas` consulta el estado y los metadatos de cierre (solo ADMINISTRADOR).

El motor asigna los valores ordinales `C=1`, `B=2`, `A=3`, `AD=4` y calcula `suma(nota × ponderación) / suma(ponderaciones con nota)`. Así, durante el período los pesos de las evaluaciones faltantes se redistribuyen proporcionalmente entre las notas ingresadas. Los umbrales iniciales son `C < 1.50`, `B >= 1.50`, `A >= 2.50` y `AD >= 3.50`; solo ADMINISTRADOR puede cambiarlos. Cada alta o corrección de nota, cambio de ponderación o regla actualiza automáticamente los resultados de períodos con actas abiertas.

Al cerrar actas, las evaluaciones sin nota y no justificadas se materializan como `C`; las justificadas siguen excluidas. Se recalculan los resultados finales antes de registrar el cierre. El cierre es explícito e independiente de las fechas del período; una vez cerrado, se bloquean las evaluaciones, notas y recálculos regulares. La acción queda registrada en auditoría.
