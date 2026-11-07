CREATE TABLE apoderados
(
    id        INT AUTO_INCREMENT PRIMARY KEY,
    active    BOOLEAN NOT NULL DEFAULT TRUE,
    persona_id INT    NOT NULL UNIQUE,
    CONSTRAINT fk_apoderado_persona
        FOREIGN KEY (persona_id) REFERENCES personas (id)
);

CREATE TABLE vinculos_familiares
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    parentesco    VARCHAR(30) NOT NULL,
    es_principal  BOOLEAN     NOT NULL DEFAULT FALSE,
    apoderado_id  INT         NOT NULL,
    estudiante_id INT         NOT NULL UNIQUE,
    CONSTRAINT fk_vinculo_familiar_apoderado
        FOREIGN KEY (apoderado_id) REFERENCES apoderados (id),
    CONSTRAINT fk_vinculo_familiar_estudiante
        FOREIGN KEY (estudiante_id) REFERENCES estudiantes (id)
);
