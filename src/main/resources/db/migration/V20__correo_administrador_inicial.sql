UPDATE personas
SET correo = 'admin@educore.edu.pe'
WHERE correo IS NULL
  AND id = (
    SELECT persona_id
    FROM cuentas_usuario
    WHERE username = 'admin@educore.edu.pe'
  );
