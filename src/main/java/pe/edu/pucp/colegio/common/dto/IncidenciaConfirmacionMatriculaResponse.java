package pe.edu.pucp.colegio.common.dto;

import java.util.List;

/** Problemas que impiden confirmar la matrícula y crear su compromiso de pago. */
public record IncidenciaConfirmacionMatriculaResponse(
    Integer matriculaId, Integer estudianteId, String nombreEstudiante, List<String> problemas) {}
