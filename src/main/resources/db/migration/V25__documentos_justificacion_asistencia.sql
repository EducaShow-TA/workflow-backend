ALTER TABLE asistencias_estudiante
    ADD COLUMN documento_justificacion_nombre VARCHAR(255);

ALTER TABLE asistencias_estudiante
    ADD COLUMN documento_justificacion_tipo VARCHAR(150);

ALTER TABLE asistencias_estudiante
    ADD COLUMN documento_justificacion_tamanio BIGINT;
