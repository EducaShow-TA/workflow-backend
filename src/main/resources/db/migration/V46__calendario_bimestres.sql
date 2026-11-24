CREATE TABLE bimestres_academicos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    anhio INT NOT NULL,
    numero INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    cerrado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_cierre DATETIME NULL,
    usuario_cierre VARCHAR(100) NULL,
    CONSTRAINT uk_bimestre_anhio_numero UNIQUE (anhio, numero),
    CONSTRAINT ck_bimestre_numero CHECK (numero BETWEEN 1 AND 4),
    CONSTRAINT ck_bimestre_fechas CHECK (fecha_inicio <= fecha_fin)
);
