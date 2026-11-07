package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;

public record AsistenciaDocenteCreateRequest(
    @NotNull Integer docenteId,
    @NotNull LocalDate fecha,
    @NotNull EstadoAsistenciaDocente estado,
    LocalTime horaEntrada,
    LocalTime horaSalida,
    @Size(max = 500) String motivoAjuste) {}
