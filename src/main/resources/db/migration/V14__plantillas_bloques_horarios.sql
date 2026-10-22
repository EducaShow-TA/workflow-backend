CREATE TABLE plantillas_horario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    nombre VARCHAR(100) NOT NULL,
    turno VARCHAR(20) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    duracion_hora_pedagogica_minutos INT NOT NULL,
    cantidad_bloques_pedagogicos INT NOT NULL,
    CONSTRAINT uk_plantilla_horario_turno_nombre UNIQUE (turno, nombre)
);

CREATE TABLE bloques_horario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plantilla_id INT NOT NULL,
    orden INT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    CONSTRAINT fk_bloque_horario_plantilla
        FOREIGN KEY (plantilla_id) REFERENCES plantillas_horario (id),
    CONSTRAINT uk_bloque_horario_plantilla_orden UNIQUE (plantilla_id, orden)
);
