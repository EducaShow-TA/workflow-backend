ALTER TABLE boletas_pension
    ADD COLUMN tipo_cargo VARCHAR(20) NOT NULL DEFAULT 'PENSION';

ALTER TABLE boletas_pension
    ADD CONSTRAINT uk_boleta_compromiso_mes_tipo
        UNIQUE (compromiso_id, mes_cobro, tipo_cargo);

ALTER TABLE boletas_pension
    DROP INDEX uk_boleta_compromiso_mes;
