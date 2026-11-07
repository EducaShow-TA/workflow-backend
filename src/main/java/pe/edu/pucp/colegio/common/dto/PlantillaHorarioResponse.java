package pe.edu.pucp.colegio.common.dto;

import java.time.LocalTime;
import java.util.List;
import pe.edu.pucp.colegio.common.model.TurnoEscolar;

public record PlantillaHorarioResponse(
    Integer id,
    String nombre,
    TurnoEscolar turno,
    LocalTime horaInicio,
    LocalTime horaFin,
    Integer duracionHoraPedagogicaMinutos,
    Integer cantidadBloquesPedagogicos,
    Boolean activo,
    List<BloqueHorarioResponse> bloques) {}
