ALTER TABLE evaluaciones_parciales
  ADD COLUMN bimestre INT NULL;

ALTER TABLE evaluaciones_parciales
  ADD CONSTRAINT ck_evaluacion_parcial_bimestre
  CHECK (bimestre IS NULL OR bimestre BETWEEN 1 AND 4);
