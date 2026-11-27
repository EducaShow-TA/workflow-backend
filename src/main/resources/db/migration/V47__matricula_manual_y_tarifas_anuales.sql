ALTER TABLE ventanas_matricula
    ADD COLUMN anhio_escolar INT NULL;

ALTER TABLE ventanas_matricula
    ADD COLUMN monto_matricula DECIMAL(12,2) NOT NULL DEFAULT 0.00;

ALTER TABLE ventanas_matricula
    ADD COLUMN monto_pension DECIMAL(12,2) NOT NULL DEFAULT 0.00;

UPDATE ventanas_matricula vm
SET anhio_escolar =
    CASE
        WHEN MONTH(vm.fecha_inicio) = 1 AND DAYOFMONTH(vm.fecha_inicio) = 1
            THEN YEAR(vm.fecha_inicio)
        WHEN EXISTS (
            SELECT 1
            FROM periodos p
            WHERE p.active = TRUE
              AND YEAR(p.fecha_inicio) = YEAR(vm.fecha_inicio)
              AND p.fecha_inicio <= vm.fecha_inicio
        ) THEN YEAR(vm.fecha_inicio) + 1
        ELSE YEAR(vm.fecha_inicio)
    END;

ALTER TABLE ventanas_matricula
    MODIFY COLUMN anhio_escolar INT NOT NULL;
