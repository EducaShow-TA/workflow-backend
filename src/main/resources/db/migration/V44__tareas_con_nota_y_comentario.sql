ALTER TABLE tareas ADD COLUMN evaluacion_parcial_id INT NULL;

ALTER TABLE tareas
  ADD CONSTRAINT fk_tarea_evaluacion_parcial
    FOREIGN KEY (evaluacion_parcial_id) REFERENCES evaluaciones_parciales(id);

ALTER TABLE tareas
  ADD CONSTRAINT uk_tarea_evaluacion_parcial UNIQUE (evaluacion_parcial_id);

ALTER TABLE calificaciones_parciales ADD COLUMN comentario VARCHAR(1000) NULL;
