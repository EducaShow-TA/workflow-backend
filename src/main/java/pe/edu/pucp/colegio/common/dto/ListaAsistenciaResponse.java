package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Lista de asistencia de una sesión con la auditoría de registro y corrección. */
public record ListaAsistenciaResponse(
    SesionClaseResumenResponse sesion,
    String registradaPor,
    LocalDateTime corregidaEn,
    String corregidaPor,
    List<FilaListaAsistenciaResponse> alumnos) {}
