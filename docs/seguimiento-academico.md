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
- `GET /api/v1/seguimiento/competencias/{codigo}?periodoId={id}` devuelve las calificaciones y los metadatos/enlace de evidencia para la ficha de seguimiento.
- Los archivos se descargan mediante `GET /api/v1/evidencias/{calificacionId}/archivo`. El límite por archivo es 10 MB.

El directorio de almacenamiento puede configurarse con la propiedad `app.storage.evidence-dir`; el valor predeterminado es `uploads/evidencias`. El acceso a estas rutas está restringido a los roles docente y administrador.
