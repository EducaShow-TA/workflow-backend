package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;
import pe.edu.pucp.colegio.common.model.TurnoEscolar;

public record PlantillaHorarioRequest(
    @NotBlank @Size(max = 100) String nombre,
    @NotNull TurnoEscolar turno,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFin,
    @NotNull @Min(1) @Max(240) Integer duracionHoraPedagogicaMinutos,
    @NotNull @Min(1) @Max(20) Integer cantidadBloquesPedagogicos,
    @NotNull @Size(max = 20) List<@Valid RecreoHorarioRequest> recreos) {}
