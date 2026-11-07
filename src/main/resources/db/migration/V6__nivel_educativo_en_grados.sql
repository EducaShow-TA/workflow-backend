ALTER TABLE grados
    ADD COLUMN nivel_educativo VARCHAR(20) NOT NULL DEFAULT 'SECUNDARIA';

ALTER TABLE grados
    DROP INDEX uk_grados_numero;

ALTER TABLE grados
    ADD CONSTRAINT uk_grado_nivel_numero
        UNIQUE (nivel_educativo, numero);
