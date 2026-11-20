CREATE TABLE comentarios_libreta (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  matricula_id INT NOT NULL,
  comentario VARCHAR(2000) NOT NULL,
  autor_cuenta_id INT NOT NULL,
  autor_nombre VARCHAR(200) NOT NULL,
  actualizado_en DATETIME NOT NULL,
  CONSTRAINT uk_comentario_libreta_matricula UNIQUE (matricula_id),
  CONSTRAINT fk_comentario_libreta_matricula FOREIGN KEY (matricula_id) REFERENCES matriculas(id),
  CONSTRAINT fk_comentario_libreta_autor FOREIGN KEY (autor_cuenta_id) REFERENCES cuentas_usuario(id)
);
