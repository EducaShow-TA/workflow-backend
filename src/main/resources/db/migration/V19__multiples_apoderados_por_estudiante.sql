CREATE TABLE vinculos_familiares_nuevo
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    parentesco    VARCHAR(30) NOT NULL,
    es_principal  BOOLEAN     NOT NULL DEFAULT FALSE,
    apoderado_id  INT         NOT NULL,
    estudiante_id INT         NOT NULL,
    CONSTRAINT uk_vinculo_estudiante_apoderado UNIQUE (estudiante_id, apoderado_id),
    CONSTRAINT fk_vinculo_nuevo_apoderado FOREIGN KEY (apoderado_id) REFERENCES apoderados (id),
    CONSTRAINT fk_vinculo_nuevo_estudiante FOREIGN KEY (estudiante_id) REFERENCES estudiantes (id)
);

INSERT INTO vinculos_familiares_nuevo
    (id, active, parentesco, es_principal, apoderado_id, estudiante_id)
SELECT id, active, parentesco, es_principal, apoderado_id, estudiante_id
FROM vinculos_familiares;

DROP TABLE vinculos_familiares;
ALTER TABLE vinculos_familiares_nuevo RENAME TO vinculos_familiares;
