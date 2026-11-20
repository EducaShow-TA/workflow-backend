CREATE TABLE configuracion_marcaje_docente (
  id INT NOT NULL PRIMARY KEY,
  tolerancia_minutos INT NOT NULL,
  ventana_antes_minutos INT NOT NULL,
  ventana_despues_minutos INT NOT NULL,
  CONSTRAINT ck_marcaje_docente_configuracion_unica CHECK (id = 1),
  CONSTRAINT ck_marcaje_docente_tolerancia CHECK (tolerancia_minutos BETWEEN 0 AND 240),
  CONSTRAINT ck_marcaje_docente_ventana_antes CHECK (ventana_antes_minutos BETWEEN 0 AND 240),
  CONSTRAINT ck_marcaje_docente_ventana_despues CHECK (ventana_despues_minutos BETWEEN 0 AND 240),
  CONSTRAINT ck_marcaje_docente_tolerancia_ventana
    CHECK (tolerancia_minutos <= ventana_despues_minutos)
);

INSERT INTO configuracion_marcaje_docente
  (id, tolerancia_minutos, ventana_antes_minutos, ventana_despues_minutos)
VALUES (1, 15, 30, 30);
