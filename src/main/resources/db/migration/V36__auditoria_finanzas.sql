CREATE TABLE auditoria_finanzas (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  fecha_hora DATETIME NOT NULL,
  usuario VARCHAR(100) NOT NULL,
  rol_usuario VARCHAR(30) NOT NULL,
  accion VARCHAR(30) NOT NULL,
  compromiso_id INT NOT NULL,
  boleta_id INT NULL,
  estudiante_id INT NOT NULL,
  estudiante_nombre VARCHAR(255) NOT NULL,
  estudiante_dni VARCHAR(15) NOT NULL,
  grado_id INT NOT NULL,
  grado_numero INT NOT NULL,
  seccion_id INT NOT NULL,
  seccion_codigo VARCHAR(50) NOT NULL,
  apoderado_id INT NOT NULL,
  apoderado_nombre VARCHAR(255) NOT NULL,
  concepto VARCHAR(120) NULL,
  mes_cobro DATE NULL,
  estado_anterior VARCHAR(20) NULL,
  estado_nuevo VARCHAR(20) NOT NULL,
  monto DECIMAL(12,2) NULL,
  numero_cuotas INT NULL,
  mecanismo VARCHAR(20) NULL,
  medio_pago VARCHAR(20) NULL,
  numero_comprobante VARCHAR(40) NULL
);

CREATE INDEX ix_auditoria_finanzas_fecha ON auditoria_finanzas(fecha_hora);
CREATE INDEX ix_auditoria_finanzas_boleta_fecha ON auditoria_finanzas(boleta_id, fecha_hora);
CREATE INDEX ix_auditoria_finanzas_estudiante_fecha ON auditoria_finanzas(estudiante_id, fecha_hora);
CREATE INDEX ix_auditoria_finanzas_accion_fecha ON auditoria_finanzas(accion, fecha_hora);
