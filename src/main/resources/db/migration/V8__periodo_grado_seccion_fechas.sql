ALTER TABLE periodos
    ADD COLUMN grado_id INT NOT NULL;

ALTER TABLE periodos
    ADD CONSTRAINT fk_periodo_grado
        FOREIGN KEY (grado_id) REFERENCES grados (id);

ALTER TABLE periodos
    ADD COLUMN seccion_id INT NOT NULL;

ALTER TABLE periodos
    ADD CONSTRAINT fk_periodo_seccion
        FOREIGN KEY (seccion_id) REFERENCES secciones (id);

ALTER TABLE periodos
    ADD CONSTRAINT uk_periodo_grado_seccion_fechas
        UNIQUE (grado_id, seccion_id, fecha_inicio, fecha_fin);
