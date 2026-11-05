CREATE TABLE docentes
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    persona_id INT     NOT NULL UNIQUE,
    CONSTRAINT fk_docente_persona
        FOREIGN KEY (persona_id) REFERENCES personas (id)
);

CREATE TABLE sesiones_clase
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    seccion_id  INT         NOT NULL,
    area_codigo VARCHAR(50) NOT NULL,
    docente_id  INT         NOT NULL,
    fecha       DATE        NOT NULL,
    hora_inicio TIME        NOT NULL,
    hora_fin    TIME        NOT NULL,
    CONSTRAINT fk_sesion_clase_seccion
        FOREIGN KEY (seccion_id) REFERENCES secciones (id),
    CONSTRAINT fk_sesion_clase_area
        FOREIGN KEY (area_codigo) REFERENCES areas (codigo),
    CONSTRAINT fk_sesion_clase_docente
        FOREIGN KEY (docente_id) REFERENCES docentes (id),
    CONSTRAINT uk_sesion_clase_seccion_area_fecha_inicio
        UNIQUE (seccion_id, area_codigo, fecha, hora_inicio),
    CONSTRAINT chk_sesion_clase_horas
        CHECK (hora_fin > hora_inicio)
);

CREATE TABLE asistencias_estudiante
(
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    active                BOOLEAN     NOT NULL DEFAULT TRUE,
    estudiante_id         INT         NOT NULL,
    sesion_clase_id       INT         NOT NULL,
    estado_asistencia     VARCHAR(20) NOT NULL,
    motivo_justificacion  VARCHAR(500),
    archivo_justificacion VARCHAR(500),
    CONSTRAINT fk_asistencia_estudiante_estudiante
        FOREIGN KEY (estudiante_id) REFERENCES estudiantes (id),
    CONSTRAINT fk_asistencia_estudiante_sesion
        FOREIGN KEY (sesion_clase_id) REFERENCES sesiones_clase (id),
    CONSTRAINT uk_asistencia_estudiante_sesion
        UNIQUE (sesion_clase_id, estudiante_id),
    CONSTRAINT chk_asistencia_estudiante_estado
        CHECK (estado_asistencia IN ('PENDIENTE_MARCAJE', 'PRESENTE', 'AUSENTE', 'TARDANZA', 'JUSTIFICADA'))
);

CREATE TABLE asistencias_docente
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    docente_id    INT         NOT NULL,
    fecha         DATE        NOT NULL,
    hora_entrada  TIME,
    hora_salida   TIME,
    estado        VARCHAR(30) NOT NULL,
    motivo_ajuste VARCHAR(500),
    CONSTRAINT fk_asistencia_docente_docente
        FOREIGN KEY (docente_id) REFERENCES docentes (id),
    CONSTRAINT uk_asistencia_docente_fecha
        UNIQUE (docente_id, fecha),
    CONSTRAINT chk_asistencia_docente_estado
        CHECK (estado IN ('PENDIENTE_MARCAJE', 'PRESENTE', 'TARDANZA', 'INASISTENCIA'))
);
