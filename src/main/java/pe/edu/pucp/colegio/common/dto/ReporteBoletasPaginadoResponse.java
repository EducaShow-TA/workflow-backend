package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;

/** Los agregados incluyen todo el filtro, independientemente de la página solicitada. */
public record ReporteBoletasPaginadoResponse(
    PaginaBoletasResponse pagina, BigDecimal montoPendiente, long familiasMorosas) {}
