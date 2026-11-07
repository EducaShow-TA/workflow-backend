-- Aulas físicas con capacidad (US-4.4)
CREATE TABLE aulas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    codigo VARCHAR(30) NOT NULL,
    capacidad INT NOT NULL,
    descripcion VARCHAR(200),
    CONSTRAINT uk_aula_codigo UNIQUE (codigo),
    CONSTRAINT chk_aula_capacidad CHECK (capacidad > 0)
);

-- Horario de una sección para un año escolar, basado en una plantilla de bloques (US-4.1)
CREATE TABLE horarios_seccion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    seccion_id INT NOT NULL,
    plantilla_id INT NOT NULL,
    anio INT NOT NULL,
    CONSTRAINT fk_horario_seccion_seccion
        FOREIGN KEY (seccion_id) REFERENCES secciones (id),
    CONSTRAINT fk_horario_seccion_plantilla
        FOREIGN KEY (plantilla_id) REFERENCES plantillas_horario (id),
    CONSTRAINT uk_horario_seccion_anio UNIQUE (seccion_id, anio)
);

-- Una sesión es una celda de la grilla: día + bloque pedagógico + área + docente + aula
CREATE TABLE sesiones_horario  (
    id INT AUTO_INCREMENT PRIMARY KEY,
    horario_id INT NOT NULL,
    bloque_id INT NOT NULL,
    dia VARCHAR(15) NOT NULL,
    area_codigo VARCHAR(50) NOT NULL,
    docente_id INT NOT NULL,
    aula_id INT NOT NULL,
    CONSTRAINT fk_sesion_horario
        FOREIGN KEY (horario_id) REFERENCES horarios_seccion (id),
    CONSTRAINT fk_sesion_bloque
        FOREIGN KEY (bloque_id) REFERENCES bloques_horario (id),
    CONSTRAINT fk_sesion_area
        FOREIGN KEY (area_codigo) REFERENCES areas (codigo),
    CONSTRAINT fk_sesion_docente
        FOREIGN KEY (docente_id) REFERENCES cuentas_usuario (id),
    CONSTRAINT fk_sesion_aula
        FOREIGN KEY (aula_id) REFERENCES aulas (id),
    CONSTRAINT uk_sesion_horario_bloque_dia UNIQUE (horario_id, bloque_id, dia),
    CONSTRAINT chk_sesion_dia
        CHECK (dia IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY')),
    INDEX idx_sesion_docente_dia (docente_id, dia),
    INDEX idx_sesion_aula_dia (aula_id, dia)
);
