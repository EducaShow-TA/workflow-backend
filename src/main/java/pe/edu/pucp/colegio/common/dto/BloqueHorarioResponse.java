package pe.edu.pucp.colegio.common.dto;

import java.time.LocalTime;
import pe.edu.pucp.colegio.common.model.TipoBloqueHorario;

public record BloqueHorarioResponse(
    Integer id, Integer orden, TipoBloqueHorario tipo, LocalTime horaInicio, LocalTime horaFin) {}
