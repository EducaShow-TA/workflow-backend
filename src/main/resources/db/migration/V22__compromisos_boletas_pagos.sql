CREATE TABLE compromisos_pago (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  matricula_id INT NOT NULL,
  apoderado_id INT NOT NULL,
  concepto VARCHAR(120) NOT NULL,
  monto_total DECIMAL(12,2) NOT NULL,
  fecha_emision DATE NOT NULL,
  mes_inicio DATE NOT NULL,
  numero_cuotas INT NOT NULL,
  primer_vencimiento DATE NOT NULL,
  dia_vencimiento INT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_compromiso_matricula UNIQUE (matricula_id),
  CONSTRAINT fk_compromiso_matricula FOREIGN KEY (matricula_id) REFERENCES matriculas(id),
  CONSTRAINT fk_compromiso_apoderado FOREIGN KEY (apoderado_id) REFERENCES apoderados(id),
  CONSTRAINT ck_compromiso_monto CHECK (monto_total > 0),
  CONSTRAINT ck_compromiso_cuotas CHECK (numero_cuotas > 0),
  CONSTRAINT ck_compromiso_dia CHECK (dia_vencimiento BETWEEN 1 AND 28)
);

CREATE TABLE boletas_pension (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  compromiso_id INT NOT NULL,
  mes_cobro DATE NOT NULL,
  monto DECIMAL(12,2) NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  mora DECIMAL(12,2) NOT NULL DEFAULT 0,
  estado_pago VARCHAR(20) NOT NULL,
  mecanismo_generacion VARCHAR(20) NOT NULL,
  fecha_generacion DATETIME NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_boleta_compromiso_mes UNIQUE (compromiso_id, mes_cobro),
  CONSTRAINT fk_boleta_compromiso FOREIGN KEY (compromiso_id) REFERENCES compromisos_pago(id),
  CONSTRAINT ck_boleta_monto CHECK (monto > 0)
);

CREATE TABLE comprobantes_pago (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  boleta_id INT NOT NULL,
  fecha_emision DATE NOT NULL,
  monto_pagado DECIMAL(12,2) NOT NULL,
  medio_pago VARCHAR(20) NOT NULL,
  numero_comprobante VARCHAR(40) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_comprobante_numero UNIQUE (numero_comprobante),
  CONSTRAINT fk_comprobante_boleta FOREIGN KEY (boleta_id) REFERENCES boletas_pension(id),
  CONSTRAINT ck_comprobante_monto CHECK (monto_pagado > 0)
);

CREATE INDEX ix_boleta_estado_vencimiento ON boletas_pension(estado_pago, fecha_vencimiento);
CREATE INDEX ix_compromiso_apoderado ON compromisos_pago(apoderado_id);
