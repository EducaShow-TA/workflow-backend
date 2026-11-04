CREATE TABLE tareas (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  periodo_id INT NOT NULL,
  competencia_codigo VARCHAR(50) NOT NULL,
  titulo VARCHAR(180) NOT NULL,
  descripcion TEXT NOT NULL,
  fecha_limite DATETIME NOT NULL,
  ponderacion DECIMAL(5,2) NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_tarea_periodo FOREIGN KEY (periodo_id) REFERENCES periodos(id),
  CONSTRAINT fk_tarea_competencia FOREIGN KEY (competencia_codigo) REFERENCES competencias(codigo),
  CONSTRAINT ck_tarea_ponderacion CHECK (ponderacion IS NULL OR ponderacion BETWEEN 0 AND 100)
);

CREATE INDEX ix_tarea_periodo_fecha ON tareas(periodo_id, fecha_limite);

CREATE TABLE entregas_tarea (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tarea_id INT NOT NULL,
  matricula_id INT NOT NULL,
  cumplida BOOLEAN NOT NULL DEFAULT FALSE,
  fecha_cumplimiento DATETIME NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_entrega_tarea_matricula UNIQUE (tarea_id, matricula_id),
  CONSTRAINT fk_entrega_tarea FOREIGN KEY (tarea_id) REFERENCES tareas(id),
  CONSTRAINT fk_entrega_matricula FOREIGN KEY (matricula_id) REFERENCES matriculas(id)
);

CREATE TABLE calificaciones (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  periodo_id INT NOT NULL,
  matricula_id INT NOT NULL,
  competencia_codigo VARCHAR(50) NOT NULL,
  nota VARCHAR(2) NOT NULL,
  observacion VARCHAR(1000) NULL,
  evidencia_url VARCHAR(2048) NULL,
  evidencia_archivo VARCHAR(500) NULL,
  evidencia_nombre VARCHAR(255) NULL,
  evidencia_tipo VARCHAR(150) NULL,
  evidencia_tamanio BIGINT NULL,
  fecha_registro DATETIME NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_calificacion_periodo_matricula_competencia UNIQUE (periodo_id, matricula_id, competencia_codigo),
  CONSTRAINT fk_calificacion_periodo FOREIGN KEY (periodo_id) REFERENCES periodos(id),
  CONSTRAINT fk_calificacion_matricula FOREIGN KEY (matricula_id) REFERENCES matriculas(id),
  CONSTRAINT fk_calificacion_competencia FOREIGN KEY (competencia_codigo) REFERENCES competencias(codigo),
  CONSTRAINT ck_calificacion_nota CHECK (nota IN ('AD', 'A', 'B', 'C')),
  CONSTRAINT ck_calificacion_evidencia CHECK (evidencia_url IS NULL OR evidencia_archivo IS NULL)
);

CREATE INDEX ix_calificacion_competencia ON calificaciones(periodo_id, competencia_codigo);
