CREATE TABLE periodos_matricula
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    active      BOOLEAN    NOT NULL DEFAULT TRUE,
    fecha_inicio DATE      NOT NULL,
    fecha_fin    DATE      NOT NULL
);

CREATE TABLE matriculas
(
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    active               BOOLEAN     NOT NULL DEFAULT TRUE,
    fecha_matricula      DATE        NOT NULL,
    estado               VARCHAR(30) NOT NULL,
    periodo_matricula_id INT         NOT NULL,
    CONSTRAINT fk_matriculas_periodo
        FOREIGN KEY (periodo_matricula_id) REFERENCES periodos_matricula (id)
);
