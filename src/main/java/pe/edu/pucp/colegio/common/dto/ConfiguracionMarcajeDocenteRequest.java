package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Ventana global de marcado de entrada y tolerancia docente, expresadas en minutos. */
public record ConfiguracionMarcajeDocenteRequest(
    @NotNull @Min(0) @Max(240) Integer toleranciaMinutos,
    @NotNull @Min(0) @Max(240) Integer ventanaAntesMinutos,
    @NotNull @Min(0) @Max(240) Integer ventanaDespuesMinutos) {}
