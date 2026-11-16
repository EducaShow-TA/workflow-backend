-- sesiones_horario.docente_id apuntaba a cuentas_usuario (V26), mientras que sesiones_clase y
-- asistencias_docente usan docentes. Se unifica: docente_id = docentes.id en todo el sistema.

-- 1) Perfil docente para las cuentas asignadas en horarios que aun no lo tienen (DocenteService
--    lo crea recien cuando se necesita).
INSERT INTO docentes (persona_id)
SELECT DISTINCT c.persona_id
FROM sesiones_horario sh
         JOIN cuentas_usuario c ON c.id = sh.docente_id
WHERE c.persona_id IS NOT NULL
  AND c.persona_id NOT IN (SELECT d.persona_id FROM docentes d);

-- 2) Cambiar la referencia de la cuenta al docente de la misma persona. Si una cuenta asignada no
--    tuviera persona, el UPDATE dejaria NULL y la migracion fallaria (docente_id es NOT NULL).
ALTER TABLE sesiones_horario DROP CONSTRAINT fk_sesion_docente;

UPDATE sesiones_horario
SET docente_id = (SELECT d.id
                  FROM docentes d
                           JOIN cuentas_usuario c ON c.persona_id = d.persona_id
                  WHERE c.id = sesiones_horario.docente_id);

ALTER TABLE sesiones_horario
    ADD CONSTRAINT fk_sesion_docente FOREIGN KEY (docente_id) REFERENCES docentes (id);
