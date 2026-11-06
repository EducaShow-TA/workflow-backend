package pe.edu.pucp.colegio.common.dto;

import java.time.DayOfWeek;
import java.util.List;
import pe.edu.pucp.colegio.common.model.TurnoEscolar;

public record HorarioSeccionResponse(
    Integer id,
    Integer seccionId,
    String seccionNombre,
    Integer anio,
    Integer plantillaId,
    String plantillaNombre,
    TurnoEscolar turno,
    List<DayOfWeek> dias,
    List<BloqueGrillaResponse> bloques) {}
