ALTER TABLE periodos_matricula
    RENAME TO periodos;

ALTER TABLE matriculas
    RENAME COLUMN periodo_matricula_id TO periodo_id;

ALTER TABLE periodos
    ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT 'PLANIFICADO';
