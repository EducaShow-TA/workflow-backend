package pe.edu.pucp.colegio.common.dto;

import pe.edu.pucp.colegio.common.model.TipoConflictoHorario;

public record ConflictoSesionResponse(TipoConflictoHorario tipo, String mensaje) {}
