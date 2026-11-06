package pe.edu.pucp.colegio.common.dto;

import java.time.LocalTime;
import java.util.List;
import pe.edu.pucp.colegio.common.model.TipoBloqueHorario;

/** Una fila de la grilla: el bloque y las sesiones asignadas en cada día. */
public record BloqueGrillaResponse(
    Integer id,
    Integer orden,
    TipoBloqueHorario tipo,
    LocalTime horaInicio,
    LocalTime horaFin,
    List<SesionClaseResponse> sesiones) {}
