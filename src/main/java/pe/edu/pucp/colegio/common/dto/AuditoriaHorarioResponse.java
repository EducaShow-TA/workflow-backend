package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AuditoriaHorarioResponse(
    Integer id,
    Integer horarioId,
    String seccion,
    String accion,
    LocalDateTime fechaHora,
    Integer usuarioCuentaId,
    String usuarioNombre,
    String detalle,
    String valoresAnteriores,
    String valoresNuevos,
    String vigenciaTipo,
    LocalDate vigenteDesde,
    LocalDate vigenteHasta) {}
