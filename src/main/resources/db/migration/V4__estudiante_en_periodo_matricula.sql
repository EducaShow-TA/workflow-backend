CREATE TABLE estudiantes
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    persona_id INT    NOT NULL UNIQUE,
    CONSTRAINT fk_estudiante_persona
        FOREIGN KEY (persona_id) REFERENCES personas (id)
);

ALTER TABLE matriculas
    ADD COLUMN estudiante_id INT NOT NULL;

ALTER TABLE matriculas
    ADD CONSTRAINT fk_matricula_estudiante
        FOREIGN KEY (estudiante_id) REFERENCES estudiantes (id);

ALTER TABLE matriculas
    ADD CONSTRAINT uk_matricula_estudiante_periodo
        UNIQUE (estudiante_id, periodo_matricula_id);
