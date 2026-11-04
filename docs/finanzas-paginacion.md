# Consultas paginadas de Finanzas

Las pantallas de Finanzas (US-1.5) y Reportes (US-1.10) consultan páginas del servidor. Los filtros,
el orden, el conteo de registros y los agregados se ejecutan en la base mediante JPA Criteria.
No se cargan todas las boletas para filtrar o paginar en memoria.

## Rutas nuevas

Todas requieren el rol ADMINISTRADOR y el prefijo `/api/v1`.

| Ruta GET | Parámetros opcionales |
| --- | --- |
| `/boletas-pension/pagina` | `estudianteId`, `periodoId`, `mes` (yyyy-MM), `estado`, `page`, `size` |
| `/reportes/boletas-pension/pagina` | `gradoId`, `seccionId`, `estado`, `vencimientoDesde`, `vencimientoHasta`, `page`, `size` |
| `/boletas-pension/estudiantes` | Sin parámetros. Devuelve estudiantes distintos con boletas activas, solo `id` y `nombre`. |

`page` empieza en 0 y su valor predeterminado es 0. `size` empieza en 1, admite hasta 100 y su
valor predeterminado es 20. Los límites inválidos producen 400. Las fechas de vencimiento son
inclusivas y un rango invertido produce 422.

Finanzas admite PENDIENTE, EN_MORA y PAGADA. El reporte de cobranza admite PENDIENTE o EN_MORA;
sin estado incluye ambos. Pedir PAGADA en el reporte paginado produce 422.

La página tiene `content`, `totalElements`, `totalPages`, `number` y `size`. El orden fijo es
vencimiento ascendente, seguido de ID ascendente para evitar saltos entre fechas iguales.

El reporte devuelve:

```json
{
  "pagina": {"content": [], "totalElements": 0, "totalPages": 0, "number": 0, "size": 20},
  "montoPendiente": 0,
  "familiasMorosas": 0
}
```

El monto es la suma de pensión y mora de todo el filtro. Las familias se cuentan con
`COUNT DISTINCT` por apoderado titular de boletas EN_MORA. Ambos valores son independientes
de la página visible y siguen disponibles si se solicita una página fuera de rango.

## Compatibilidad e integración

Los endpoints anteriores `/boletas-pension` y `/reportes/boletas-pension` conservan sus contratos
sin paginar para los clientes existentes. Ahora también filtran en SQL, pero continúan devolviendo
todos sus resultados para mantener compatibilidad. La exclusión predeterminada de pagadas se
aplica al reporte paginado; la ruta antigua conserva su semántica anterior.

No se modifican los endpoints de detalle, registro de pagos y comprobantes, ni las reglas de pago.
No hay migraciones nuevas. El frontend usa las rutas paginadas y el catálogo ligero de estudiantes.

Reiniciar el backend después de compilar y recargar el frontend para usar las nuevas rutas. Los
datos de prueba locales no se incluyen en este cambio.

## Verificación

`FinanzasPaginacionIntegrationTest` comprueba filtros combinados, orden estable, límites inclusivos,
familias sin duplicados, agregados independientes de página, exclusión de inactivos y pagadas,
contratos anteriores, páginas vacías y autorización. Usa H2 aislada con transacciones revertidas.
