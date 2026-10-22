INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 1, 'SECUNDARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 1 AND nivel_educativo = 'SECUNDARIA'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 2, 'SECUNDARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 2 AND nivel_educativo = 'SECUNDARIA'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 3, 'SECUNDARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 3 AND nivel_educativo = 'SECUNDARIA'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 4, 'SECUNDARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 4 AND nivel_educativo = 'SECUNDARIA'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 5, 'SECUNDARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 5 AND nivel_educativo = 'SECUNDARIA'
);
