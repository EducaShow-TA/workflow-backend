ALTER TABLE periodos
    ADD COLUMN catalogo_libreta_snapshot BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE periodo_catalogo_libreta_snapshot (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    periodo_id INT NOT NULL,
    area_codigo VARCHAR(50) NOT NULL,
    area_nombre VARCHAR(255) NOT NULL,
    competencia_codigo VARCHAR(50) NOT NULL,
    competencia_nombre VARCHAR(255) NOT NULL,
    CONSTRAINT uk_periodo_catalogo_libreta_snapshot
        UNIQUE (periodo_id, area_codigo, competencia_codigo),
    CONSTRAINT fk_periodo_catalogo_libreta_snapshot
        FOREIGN KEY (periodo_id) REFERENCES periodos(id)
);

INSERT INTO periodo_catalogo_libreta_snapshot (
    periodo_id, area_codigo, area_nombre, competencia_codigo, competencia_nombre)
SELECT p.id, a.codigo, a.nombre, c.codigo, c.nombre
FROM periodos p
CROSS JOIN areas_competencias ac
JOIN areas a ON a.codigo = ac.area_codigo AND a.active = TRUE
JOIN competencias c ON c.codigo = ac.competencia_codigo AND c.active = TRUE
WHERE p.actas_cerradas = TRUE;

UPDATE periodos SET catalogo_libreta_snapshot = TRUE WHERE actas_cerradas = TRUE;
