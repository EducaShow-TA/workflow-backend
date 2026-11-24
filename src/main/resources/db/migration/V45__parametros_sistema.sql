CREATE TABLE parametros_sistema (
    id INT NOT NULL PRIMARY KEY,
    desfase_segundos BIGINT NULL,
    motivo_fecha VARCHAR(300) NULL,
    fecha_simulada_por VARCHAR(100) NULL,
    fecha_simulada_en DATETIME NULL,
    pago_en_linea_activo BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO parametros_sistema (id, pago_en_linea_activo) VALUES (1, TRUE);
