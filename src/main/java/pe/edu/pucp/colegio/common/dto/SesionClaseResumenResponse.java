package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Sesión de clase con el avance de su asistencia y su ventana de marcado. */
public record SesionClaseResumenResponse(
    Integer id,
    Integer seccionId,
    String seccionCodigo,
    Integer gradoNumero,
    String areaCodigo,
    String areaNombre,
    Integer docenteId,
    String docenteNombre,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    int totalAlumnos,
    int registrados,
    int presentes,
    int tardanzas,
    int ausentes,
    int justificadas,
    LocalDateTime ventanaApertura,
    LocalDateTime ventanaCierre,
    boolean ventanaAbierta,
    LocalDateTime registradaEn) {}
