CREATE TABLE auditoria_horario (
    id INT NOT NULL AUTO_INCREMENT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    horario_id INT NOT NULL,
    seccion VARCHAR(120) NOT NULL,
    accion VARCHAR(20) NOT NULL,
    fecha_hora DATETIME NOT NULL,
    usuario_cuenta_id INT NULL,
    usuario_nombre VARCHAR(160) NOT NULL,
    detalle TEXT NOT NULL,
    valores_anteriores TEXT NULL,
    valores_nuevos TEXT NULL,
    vigencia_tipo VARCHAR(20) NOT NULL,
    vigente_desde DATE NULL,
    vigente_hasta DATE NULL,
    PRIMARY KEY (id),
    INDEX idx_auditoria_horario_fecha (horario_id, fecha_hora),
    INDEX idx_auditoria_horario_seccion (seccion, fecha_hora)
);
