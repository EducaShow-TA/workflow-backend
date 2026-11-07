CREATE TABLE areas
(
    codigo      VARCHAR(50)  NOT NULL PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE competencias
(
    codigo      VARCHAR(50)  NOT NULL PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE areas_competencias
(
    area_codigo        VARCHAR(50) NOT NULL,
    competencia_codigo VARCHAR(50) NOT NULL,
    PRIMARY KEY (area_codigo, competencia_codigo),
    CONSTRAINT fk_areas_competencias_area
        FOREIGN KEY (area_codigo) REFERENCES areas (codigo),
    CONSTRAINT fk_areas_competencias_competencia
        FOREIGN KEY (competencia_codigo) REFERENCES competencias (codigo)
);
