ALTER TABLE sesiones_clase ADD COLUMN sesion_horario_id INT NULL;
ALTER TABLE sesiones_clase ADD COLUMN cancelada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sesiones_clase ADD COLUMN motivo_cancelacion VARCHAR(300) NULL;
ALTER TABLE sesiones_clase
    ADD CONSTRAINT fk_sesion_clase_sesion_horario
        FOREIGN KEY (sesion_horario_id) REFERENCES sesiones_horario (id);

ALTER TABLE sesiones_clase
    ADD CONSTRAINT uk_sesion_clase_origen_fecha UNIQUE (sesion_horario_id, fecha);

CREATE INDEX idx_sesion_clase_docente_fecha
    ON sesiones_clase (docente_id, fecha, active, cancelada);

CREATE TABLE dias_no_lectivos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    fecha DATE NOT NULL,
    descripcion VARCHAR(120) NOT NULL,
    CONSTRAINT uk_dia_no_lectivo_fecha UNIQUE (fecha)
);

ALTER TABLE sesiones_horario ADD COLUMN vigente_desde DATE NULL;
ALTER TABLE sesiones_horario ADD COLUMN vigente_hasta DATE NULL;

UPDATE sesiones_horario sh
SET vigente_desde = CAST(
    CONCAT((SELECT hs.anio FROM horarios_seccion hs WHERE hs.id = sh.horario_id), '-01-01')
    AS DATE
)
WHERE vigente_desde IS NULL;

ALTER TABLE sesiones_horario MODIFY COLUMN vigente_desde DATE NOT NULL;
ALTER TABLE sesiones_horario
    ADD CONSTRAINT uk_sesion_horario_vigencia
        UNIQUE (horario_id, bloque_id, dia, vigente_desde);

-- El nuevo índice comienza por horario_id y reemplaza el soporte de la FK existente.
ALTER TABLE sesiones_horario DROP INDEX uk_sesion_horario_bloque_dia;
