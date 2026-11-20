ALTER TABLE entregas_tarea ADD COLUMN archivo_clave VARCHAR(500) NULL;
ALTER TABLE entregas_tarea ADD COLUMN archivo_nombre VARCHAR(255) NULL;
ALTER TABLE entregas_tarea ADD COLUMN archivo_tipo VARCHAR(150) NULL;
ALTER TABLE entregas_tarea ADD COLUMN archivo_tamanio BIGINT NULL;
ALTER TABLE entregas_tarea ADD COLUMN archivo_fecha DATETIME NULL;
ALTER TABLE entregas_tarea ADD COLUMN origen VARCHAR(20) NULL;

ALTER TABLE entregas_tarea
  ADD CONSTRAINT ck_entrega_tarea_origen CHECK (origen IS NULL OR origen IN ('ALUMNO', 'DOCENTE'));
