ALTER TABLE registros_error
    ADD COLUMN modulo VARCHAR(50);

ALTER TABLE registros_error
    MODIFY COLUMN stack_trace VARCHAR(4000);

CREATE INDEX idx_registros_error_fecha ON registros_error (fecha_hora);