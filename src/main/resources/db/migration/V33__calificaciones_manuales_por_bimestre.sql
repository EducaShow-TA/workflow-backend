ALTER TABLE calificaciones ADD COLUMN bimestre INT NULL;

ALTER TABLE calificaciones DROP INDEX uk_calificacion_periodo_matricula_competencia;

ALTER TABLE calificaciones
  ADD CONSTRAINT uk_calificacion_periodo_matricula_competencia
  UNIQUE (periodo_id, matricula_id, competencia_codigo, bimestre);

ALTER TABLE calificaciones
  ADD CONSTRAINT ck_calificacion_bimestre
  CHECK (bimestre IS NULL OR bimestre BETWEEN 1 AND 4);

ALTER TABLE auditoria_calificaciones ADD COLUMN bimestre INT NULL;
