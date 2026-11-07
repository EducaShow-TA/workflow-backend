ALTER TABLE periodos ADD COLUMN actas_cerradas BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE periodos ADD COLUMN fecha_cierre_actas DATETIME NULL;
ALTER TABLE periodos ADD COLUMN usuario_cierre_actas VARCHAR(100) NULL;

ALTER TABLE calificaciones ADD COLUMN puntaje_consolidado DECIMAL(6,4) NULL;
ALTER TABLE calificaciones ADD COLUMN consolidada BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE reglas_consolidacion (
  competencia_codigo VARCHAR(50) NOT NULL PRIMARY KEY,
  umbral_b DECIMAL(4,2) NOT NULL DEFAULT 1.50,
  umbral_a DECIMAL(4,2) NOT NULL DEFAULT 2.50,
  umbral_ad DECIMAL(4,2) NOT NULL DEFAULT 3.50,
  CONSTRAINT fk_regla_consolidacion_competencia
    FOREIGN KEY (competencia_codigo) REFERENCES competencias(codigo),
  CONSTRAINT ck_regla_consolidacion_umbrales
    CHECK (umbral_b > 1 AND umbral_b < umbral_a AND umbral_a < umbral_ad AND umbral_ad <= 4)
);

CREATE TABLE evaluaciones_parciales (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  periodo_id INT NOT NULL,
  competencia_codigo VARCHAR(50) NOT NULL,
  titulo VARCHAR(180) NOT NULL,
  descripcion VARCHAR(1000) NULL,
  ponderacion DECIMAL(5,2) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_evaluacion_parcial_periodo
    FOREIGN KEY (periodo_id) REFERENCES periodos(id),
  CONSTRAINT fk_evaluacion_parcial_competencia
    FOREIGN KEY (competencia_codigo) REFERENCES competencias(codigo),
  CONSTRAINT ck_evaluacion_parcial_ponderacion CHECK (ponderacion > 0 AND ponderacion <= 100)
);

CREATE INDEX ix_evaluacion_parcial_periodo_competencia
  ON evaluaciones_parciales(periodo_id, competencia_codigo, active);

CREATE TABLE calificaciones_parciales (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  evaluacion_id INT NOT NULL,
  matricula_id INT NOT NULL,
  nota VARCHAR(2) NULL,
  justificada BOOLEAN NOT NULL DEFAULT FALSE,
  evidencia_url VARCHAR(2048) NULL,
  evidencia_archivo VARCHAR(500) NULL,
  evidencia_nombre VARCHAR(255) NULL,
  evidencia_tipo VARCHAR(150) NULL,
  evidencia_tamanio BIGINT NULL,
  fecha_registro DATETIME NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_calificacion_parcial_evaluacion_matricula UNIQUE (evaluacion_id, matricula_id),
  CONSTRAINT fk_calificacion_parcial_evaluacion
    FOREIGN KEY (evaluacion_id) REFERENCES evaluaciones_parciales(id),
  CONSTRAINT fk_calificacion_parcial_matricula
    FOREIGN KEY (matricula_id) REFERENCES matriculas(id),
  CONSTRAINT ck_calificacion_parcial_nota CHECK (nota IS NULL OR nota IN ('AD', 'A', 'B', 'C')),
  CONSTRAINT ck_calificacion_parcial_justificacion CHECK (
    (justificada = TRUE AND nota IS NULL) OR (justificada = FALSE AND nota IS NOT NULL)
  ),
  CONSTRAINT ck_calificacion_parcial_evidencia CHECK (
    evidencia_url IS NULL OR evidencia_archivo IS NULL
  )
);

CREATE INDEX ix_calificacion_parcial_matricula ON calificaciones_parciales(matricula_id, active);
