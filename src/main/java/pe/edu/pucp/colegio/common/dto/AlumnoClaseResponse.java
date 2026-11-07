package pe.edu.pucp.colegio.common.dto;

/** Alumno con matrícula confirmada en una clase del docente. */
public record AlumnoClaseResponse(
    Integer matriculaId, Integer estudianteId, String dni, String nombreCompleto) {}
