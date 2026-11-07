CREATE TABLE grados
(
    id     INT AUTO_INCREMENT PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    numero INT     NOT NULL,
    CONSTRAINT uk_grados_numero UNIQUE (numero)
);

CREATE TABLE secciones
(
    id               INT AUTO_INCREMENT PRIMARY KEY,
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    codigo           VARCHAR(50) NOT NULL,
    vacantes_totales VARCHAR(20),
    grado_id         INT         NOT NULL,
    CONSTRAINT fk_seccion_grado
        FOREIGN KEY (grado_id) REFERENCES grados (id),
    CONSTRAINT uk_seccion_grado_codigo
        UNIQUE (grado_id, codigo)
);
