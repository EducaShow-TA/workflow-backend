-- Quién y cuándo registró o corrigió la asistencia de cada alumno (HU 2.1).
ALTER TABLE asistencias_estudiante ADD COLUMN registrada_en DATETIME NULL;
ALTER TABLE asistencias_estudiante ADD COLUMN registrada_por VARCHAR(100) NULL;
ALTER TABLE asistencias_estudiante ADD COLUMN corregida_en DATETIME NULL;
ALTER TABLE asistencias_estudiante ADD COLUMN corregida_por VARCHAR(100) NULL;
