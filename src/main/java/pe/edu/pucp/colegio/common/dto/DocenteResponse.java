package pe.edu.pucp.colegio.common.dto;

/** Docente disponible para asignar sesiones de clase. */
public record DocenteResponse(
    Integer id, Integer cuentaId, String username, String nombreCompleto, String dni) {}
