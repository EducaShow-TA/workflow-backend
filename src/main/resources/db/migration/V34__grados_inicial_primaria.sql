INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 3, 'INICIAL'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 3 AND nivel_educativo = 'INICIAL'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 4, 'INICIAL'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 4 AND nivel_educativo = 'INICIAL'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 5, 'INICIAL'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 5 AND nivel_educativo = 'INICIAL'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 1, 'PRIMARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 1 AND nivel_educativo = 'PRIMARIA'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 2, 'PRIMARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 2 AND nivel_educativo = 'PRIMARIA'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 3, 'PRIMARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 3 AND nivel_educativo = 'PRIMARIA'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 4, 'PRIMARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 4 AND nivel_educativo = 'PRIMARIA'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 5, 'PRIMARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 5 AND nivel_educativo = 'PRIMARIA'
);

INSERT INTO grados (active, numero, nivel_educativo)
SELECT TRUE, 6, 'PRIMARIA'
WHERE NOT EXISTS (
    SELECT 1 FROM grados WHERE numero = 6 AND nivel_educativo = 'PRIMARIA'
);
