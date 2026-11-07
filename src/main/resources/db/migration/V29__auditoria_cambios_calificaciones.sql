CREATE TABLE auditoria_calificaciones (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  fecha_hora DATETIME NOT NULL,
  usuario VARCHAR(100) NOT NULL,
  rol_usuario VARCHAR(30) NOT NULL,
  accion VARCHAR(40) NOT NULL,
  tipo_calificacion VARCHAR(20) NOT NULL,
  periodo_id INT NOT NULL,
  matricula_id INT NOT NULL,
  estudiante_id INT NOT NULL,
  competencia_codigo VARCHAR(50) NOT NULL,
  evaluacion_id INT NULL,
  nota_anterior VARCHAR(2) NULL,
  nota_nueva VARCHAR(2) NULL,
  puntaje_anterior DECIMAL(6,4) NULL,
  puntaje_nuevo DECIMAL(6,4) NULL,
  consolidada_anterior BOOLEAN NULL,
  consolidada_nueva BOOLEAN NULL,
  justificada_anterior BOOLEAN NULL,
  justificada_nueva BOOLEAN NULL,
  observacion_anterior VARCHAR(1000) NULL,
  observacion_nueva VARCHAR(1000) NULL
);

CREATE INDEX ix_auditoria_calificaciones_periodo_fecha
  ON auditoria_calificaciones(periodo_id, fecha_hora);
CREATE INDEX ix_auditoria_calificaciones_matricula_fecha
  ON auditoria_calificaciones(matricula_id, fecha_hora);
CREATE INDEX ix_auditoria_calificaciones_estudiante_fecha
  ON auditoria_calificaciones(estudiante_id, fecha_hora);
CREATE INDEX ix_auditoria_calificaciones_competencia_fecha
  ON auditoria_calificaciones(competencia_codigo, fecha_hora);
