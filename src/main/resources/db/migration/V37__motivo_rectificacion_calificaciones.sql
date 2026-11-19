ALTER TABLE auditoria_calificaciones ADD COLUMN motivo VARCHAR(500) NULL;

CREATE INDEX ix_auditoria_calificaciones_nota
  ON auditoria_calificaciones (matricula_id, competencia_codigo, tipo_calificacion, bimestre);
