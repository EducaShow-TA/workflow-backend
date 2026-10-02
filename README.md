# EducaShow — prototipo de autenticación

Backend Spring Boot 3 para el prototipo de arquitectura del módulo de seguridad. Implementa el contrato `POST /api/v1/auth/login`, JWT Bearer, bloqueo temporal después de intentos fallidos y migración inicial Flyway para MySQL.

## Configuración

El servicio requiere Java 17 y una base MySQL. No se versionan secretos: defina estas variables antes de iniciar la aplicación:

```bash
export DB_URL='jdbc:mysql://localhost:3306/colegio_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USERNAME='root'
export DB_PASSWORD='...'
export JWT_SECRET='base64-de-una-clave-aleatoria-de-al-menos-256-bits'
```

También se pueden configurar `PORT`, `JWT_EXPIRATION_MS`, `MAX_LOGIN_ATTEMPTS` y `LOCK_TIME_MINUTES`. Flyway ejecuta `V1__init_security.sql` al iniciar y crea el administrador `admin@educore.edu.pe` con la contraseña indicada en la guía del proyecto.

## Verificación manual

```bash
mvn spring-boot:run
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin@educore.edu.pe","password":"Admin2026!"}'
```

Use el `token` recibido como `Authorization: Bearer <token>` para `GET /api/v1/auth/me`.
