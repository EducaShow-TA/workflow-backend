package pe.edu.pucp.colegio.common.dto;

import java.util.List;

/** Resultado de validar una asignación sin guardarla. */
public record ValidacionSesionResponse(boolean valida, List<ConflictoSesionResponse> conflictos) {}
