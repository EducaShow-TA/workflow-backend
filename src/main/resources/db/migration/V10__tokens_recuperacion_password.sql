CREATE TABLE tokens_recuperacion_password
(
    id INT AUTO_INCREMENT PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    cuenta_usuario_id INT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    creado_en DATETIME NOT NULL,
    expira_en DATETIME NOT NULL,
    usado_en DATETIME NULL,

    CONSTRAINT fk_token_recuperacion_cuenta
        FOREIGN KEY (cuenta_usuario_id)
        REFERENCES cuentas_usuario (id),

    INDEX idx_token_recuperacion_cuenta
        (cuenta_usuario_id, active)
);