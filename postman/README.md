# Pruebas de API con Postman

Esta carpeta contiene una colección de solicitudes encadenadas para revisar las APIs implementadas del Sprint 1:

- `EducaShow-Sprint1.postman_collection.json`: solicitudes y validaciones automatizadas.
- `EducaShow-Sprint1.postman_environment.json`: URL local, credenciales de desarrollo y variables que la colección completa durante la ejecución.

## Requisitos y preparación

1. Inicia el backend en `http://localhost:8080` y verifica que Flyway haya aplicado todas las migraciones incluidas en esta rama.
2. Importa ambos JSON en Postman y selecciona el environment **EducaShow - Local**.
3. Ejecuta la colección en orden con **Run**. La solicitud de login obtiene el JWT y la colección guarda los IDs que generan las solicitudes posteriores; no es necesario rellenarlos manualmente.

Se recomienda ejecutar la colección contra una **base de datos local de pruebas recién creada** (esquema aplicado desde cero por Flyway y datos semilla). La colección presupone que `gradoId=1` existe y corresponde al primer grado de secundaria sembrado por la migración V12. En una base existente o personalizada, confirma ese ID y actualiza `gradoId` en el environment.

No restaures ni borres una base que contenga información que quieras conservar. Si necesitas repetir el flujo desde limpio, usa una base aislada de desarrollo/pruebas y vuelve a crearla desde Flyway.

## Credenciales y correo de recuperación

La colección usa el administrador semilla local:

| Variable | Valor inicial |
| --- | --- |
| `adminUser` | `admin@educore.edu.pe` |
| `adminPassword` | `Admin2026!` |

Son credenciales exclusivamente de desarrollo. Si la contraseña fue cambiada en esa base, actualiza `adminPassword` en el environment.

La dirección del administrador es un correo de prueba, no un buzón real supervisado. Por eso `Solicitar recuperación` valida la respuesta genérica del endpoint, pero **no se espera recibir un correo ni completar el restablecimiento** en la configuración local predeterminada. La solicitud `Restablecer contraseña` se omite automáticamente mientras `resetToken` conserve el valor de marcador. Solo reemplázalo por un token real cuando hayas configurado correo saliente y puedas leer el mensaje recibido.

## Qué ejecuta la colección

Las carpetas se deben ejecutar en el orden presentado:

1. **Autenticación**: inicia sesión como administrador y guarda el Bearer token; solicita recuperación de contraseña. El restablecimiento queda omitido sin token real.
2. **Catálogo curricular**: crea un área con competencias, edita el área, añade/edita una competencia y consulta/desactiva recursos según el flujo.
3. **Vacantes por sección**: crea una sección de prueba en el grado configurado, crea un periodo académico, registra dos estudiantes y los matricula. Comprueba que reducir la capacidad por debajo de las dos matrículas resulte en `422`; luego elimina las matrículas de prueba.
4. **Cuentas y roles**: prueba operaciones de cuentas protegidas con usuario administrador.
5. **Registro de errores**: provoca/consulta los errores previstos y verifica el listado y el detalle de logs.

La colección crea datos de prueba. Aunque elimina las matrículas creadas en el caso de capacidad, conserva las cuentas, estudiantes, áreas, competencias, secciones y periodos generados. Cada ejecución puede añadir nuevos registros. Para resultados previsibles y evitar acumular datos, vuelve a una base aislada limpia antes de repetirla. La creación de secciones busca nombres libres; una base con muchas secciones puede agotar las combinaciones disponibles.

## Resultados y límites conocidos

- Un error de Postman como `Detalle de log (incluye stackTrace)` puede depender del tipo de registro: la traza técnica solo está presente cuando el backend capturó una excepción, especialmente para errores internos `500`; no todos los errores `4xx` tienen `stackTrace`. Para verificar el criterio de que un `500` se guarda con traza sin exponerla al cliente, consulta la prueba de integración `RegistroError500IntegrationTest`.
- La recuperación por correo no es una prueba de extremo a extremo hasta configurar un SMTP y un destinatario real/de prueba accesible.
- Esta colección cubre los endpoints actuales. No implica que US-1.1 (ventana independiente de matrícula), US-4.3 (plantillas horarias) o US-5.9 (importación CSV) estén implementadas.
- US-1.7 utiliza secciones, periodos académicos y matrículas existentes. El modelo de periodo académico ligado a grado/sección no equivale por sí mismo a la ventana de matrícula descrita en US-1.1.

Para el estado del backend por historia y el contrato de las rutas, consulta [`../README.md`](../README.md).
