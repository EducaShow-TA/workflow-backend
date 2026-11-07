CREATE TABLE ventanas_matricula (
    id INT NOT NULL AUTO_INCREMENT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_inicio DATE NOT NULL,
    fecha_limite DATE NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_ventanas_matricula_vigencia (active, fecha_inicio, fecha_limite),
    CONSTRAINT chk_ventana_matricula_fechas CHECK (fecha_limite > fecha_inicio)
);
