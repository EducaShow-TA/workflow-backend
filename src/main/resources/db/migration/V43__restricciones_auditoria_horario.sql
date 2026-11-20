-- US-4.5: restricciones que faltaron en V41 (EP §5.1: FK con FOREIGN KEY y CHECK para los valores fijos).
-- Nombres con prefijo aud_horario para no chocar con restricciones de otras tablas (son únicos por base en MySQL).
ALTER TABLE auditoria_horario
    ADD CONSTRAINT fk_aud_horario_horario FOREIGN KEY (horario_id) REFERENCES horarios_seccion (id);
ALTER TABLE auditoria_horario
    ADD CONSTRAINT fk_aud_horario_usuario FOREIGN KEY (usuario_cuenta_id) REFERENCES cuentas_usuario (id);
ALTER TABLE auditoria_horario
    ADD CONSTRAINT ck_aud_horario_accion CHECK (accion IN ('CREACIÓN', 'ACTUALIZACIÓN', 'ELIMINACIÓN'));
ALTER TABLE auditoria_horario
    ADD CONSTRAINT ck_aud_horario_vigencia CHECK (vigencia_tipo IN ('PERMANENTE', 'TEMPORAL'));
