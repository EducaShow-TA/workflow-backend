CREATE TABLE configuracion_riesgo_academico (
  id INT NOT NULL PRIMARY KEY,
  umbral INT NOT NULL,
  CONSTRAINT ck_riesgo_academico_umbral CHECK (umbral BETWEEN 1 AND 5)
);

INSERT INTO configuracion_riesgo_academico (id, umbral) VALUES (1, 1);
