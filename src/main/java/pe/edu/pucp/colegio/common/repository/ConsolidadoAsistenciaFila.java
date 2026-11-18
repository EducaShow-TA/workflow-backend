package pe.edu.pucp.colegio.common.repository;

import pe.edu.pucp.colegio.common.model.NivelEducativo;

/** Conteo de asistencia de un alumno en una sección, agregado por la base de datos (US-2.6). */
public interface ConsolidadoAsistenciaFila {

  Integer getEstudianteId();

  String getNombre();

  String getApellidoPaterno();

  String getApellidoMaterno();

  Integer getGradoId();

  Integer getGradoNumero();

  NivelEducativo getNivelEducativo();

  Integer getSeccionId();

  String getSeccionCodigo();

  Long getPresentes();

  Long getTardanzas();

  Long getInasistencias();

  Long getJustificadas();
}
