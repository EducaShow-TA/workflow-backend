# Casos de prueba — Primera iteración de EducaShow

**Paquete:** 3.1.2 · **Iteración:** 1 · **Corte de revisión:** 23/10/2026  
**Alcance:** nueve historias comprometidas en Sprint 1, verificaciones de backend/API y validación manual reportada de la interfaz.

> Los resultados manuales consignados reflejan lo reportado por el equipo durante la revisión. Adjuntar las capturas o exportaciones originales del Runner antes de entregar. No se adjuntaron evidencias binarias a este documento.

## 1. Entorno y convenciones

| Elemento | Entorno usado / requerido |
| --- | --- |
| Backend | Spring Boot, ejecutado localmente en `http://localhost:8080` |
| Base de datos | MySQL local con migraciones Flyway aplicadas; para pruebas automatizadas se usa H2 |
| Frontend | Nuxt 3 local, configurado para consumir el backend local |
| API | Postman, colección `postman/EducaShow-Sprint1.postman_collection.json` y environment asociado |
| Cuenta | Administrador local; no incluir contraseña, JWT ni tokens en evidencias compartidas |

Estados: **Aprobado** = ejecución reportada satisfactoria; **No aprobado** = defecto reproducido; **Pendiente** = no hay evidencia suficiente o falta repetir la ejecución tras cambios. “Automatizado” identifica el mecanismo, no sustituye el estado.

## 2. Casos de API y backend

| ID | Historia / escenario | Pasos y resultado esperado | Resultado / evidencia |
| --- | --- | --- | --- |
| BE-01 | US-5.6 Login válido | Enviar credenciales correctas a `POST /api/v1/auth/login`; recibir `200`, token Bearer y perfil de administrador. | **Aprobado.** Login local comprobado y colección Postman. Adjuntar captura del Runner con token oculto. |
| BE-02 | US-5.6 Credenciales inválidas y bloqueo | Probar contraseña incorrecta y exceder intentos; comprobar `401`/bloqueo y que la clave correcta no evada el bloqueo temporal. | **Aprobado en automatización.** `AuthIntegrationTest`; adjuntar resumen Surefire o captura. |
| BE-03 | US-5.2 Autenticación y autorización | Probar ruta protegida sin sesión y ruta administrativa con rol insuficiente; esperar `401` y `403`. | **Aprobado en automatización/Postman.** Adjuntar respuestas redactadas sin token. |
| BE-04 | US-5.2 Crear, listar y editar cuentas | Crear usuario, listar y actualizar datos permitidos; verificar persistencia y que rol/contraseña/DNI no cambien al editar. | **Aprobado según Postman/prueba del módulo.** Adjuntar Runner y datos anonimizados. |
| BE-05 | US-5.2 Activar/desactivar cuenta | Desactivar cuenta de prueba; probar login y token anterior; reactivar y repetir. | **Aprobado según Postman y prueba manual de usuarios.** Adjuntar captura del estado y respuesta de login, ocultando credenciales. |
| BE-06 | US-5.9 Carga masiva CSV | Descargar plantilla; importar filas APODERADO, ALUMNO asociado y DOCENTE; comprobar procesadas, rechazadas, cuenta y vínculo familiar. | **Aprobado según ejecución reportada de Postman y pruebas automatizadas CSV.** Adjuntar resumen Runner y resultado de carga. |
| BE-07 | US-5.6 Recuperación de contraseña | Solicitar enlace por correo del perfil y completar el cambio con token recibido; probar token de un solo uso. | **Pendiente / no aprobado para aceptación de extremo a extremo.** Se detectó discrepancia entre correo y `username`; la corrección reciente no se ha revalidado. Pruebas previas del backend no acreditan el cambio recién hecho. |
| BE-08 | US-3.5 Catálogo curricular | Crear/consultar/editar área y competencia; cambiar estado y validar conflictos. | **Aprobado según Postman y pruebas del catálogo.** Adjuntar Runner. |
| BE-09 | US-1.1 Ventana de matrícula | Crear ventanas pendiente, vigente y vencida; comprobar fechas, actualización, desactivación y rechazo de límites inválidos. | **Aprobado según Postman.** Se corrigieron expectativas/rangos que chocaban; repetir con la colección final y adjuntar salida actualizada. |
| BE-10 | US-1.2 Matrícula académica | Registrar, confirmar, rechazar duplicado y retirar matrícula; verificar bloqueo cuando no hay ventana vigente. | **Aprobado según Postman y prueba manual reportada.** Adjuntar respuestas y estado de vacantes. |
| BE-11 | US-1.7 Secciones | Crear sección para un grado, listar y modificar capacidad; comprobar duplicados y valores inválidos. | **Aprobado según Postman y pruebas de integración.** Adjuntar Runner. |
| BE-12 | US-1.7 Vacantes | Consultar vacantes antes/después de matricular; comprobar que no sean negativas y que capacidad inferior a ocupación se rechace. | **Aprobado según Postman y prueba manual reportada.** Adjuntar captura de conteo antes/después. |
| BE-13 | US-4.3 Bloques y franjas horarias | Crear/consultar/editar/desactivar plantilla; rechazar duración total superior al turno. | **Aprobado según Postman, prueba de integración y prueba manual reportada.** Adjuntar Runner/capturas. |
| BE-14 | US-5.5 Log de errores | Consultar registros y detalle; comprobar persistencia de un error interno controlado y no exposición de stack trace al cliente. | **Aprobado en pruebas automatizadas/Postman según el registro del proyecto; la revisión manual de la pantalla no está confirmada.** Adjuntar respuesta del listado/detalle y log redactado. |
| BE-15 | Reglas transversales de API | Repetir solicitudes con campos inválidos, permisos insuficientes, recursos inexistentes y conflictos. | **Aprobado en suites automatizadas y colección, excepto cambios recientes de recuperación que quedan pendientes.** Adjuntar resultados y códigos esperados/recibidos. |

### Escenarios negativos independientes requeridos por la retroalimentación

En esta tabla, “Evidencia” identifica el artefacto que debe guardarse al ejecutar el caso. Los estados permanecen **Pendiente de ejecución/evidencia** hasta adjuntar el resultado de la versión entregada.

| ID | HU asociada | Datos de entrada concretos / precondición | Resultado esperado verificable | Evidencia |
| --- | --- | --- | --- | --- |
| BE-01a | US-5.6 | Usuario `admin01` activo y contraseña correcta; `POST /api/v1/auth/login`. | HTTP 200; respuesta contiene JWT Bearer y perfil ADMINISTRADOR. | Postman: guardar respuesta 200 con JWT oculto; captura del caso en Runner. |
| BE-01b | US-5.6 | Usuario inexistente `no-existe-qa-2026`, contraseña `Prueba!234`; login. | HTTP 401; mensaje `No encontramos una cuenta con ese usuario o correo.`; no se emite token. | Postman: guardar cuerpo/status 401 y captura de `/login` con el mensaje. |
| BE-01c | US-5.1 / US-5.6 | Cuenta `qa.inactivo`, precreada y desactivada por administrador; enviar su contraseña correcta. | HTTP 401; mensaje `Tu cuenta está desactivada. Contacta al administrador del colegio.`; no se emite token. | Postman: evidencia del estado inactivo y respuesta 401; captura del mensaje en `/login`. |
| BE-01d | US-5.6 / US-5.7 | Cuenta activa; enviar contraseña incorrecta hasta alcanzar el límite configurado y luego enviar la correcta durante el bloqueo. | HTTP 401 en ambos intentos; el segundo mensaje indica bloqueo temporal; no se emite token. | Postman/AuthIntegrationTest: respuestas de intentos y captura de mensaje de bloqueo. |
| BE-09a | US-1.1 | `POST /api/v1/periodos-matricula`, hoy a hoy + 10 días (fechas calculadas al ejecutar). | HTTP 201; mensaje de éxito y `estado=VIGENTE`; retorna ID. | Postman: request/response de `Configurar ventana de matrícula vigente`; captura Runner. |
| BE-09b | US-1.1 | `POST`; `fechaInicio=2099-01-10`, `fechaLimite=2099-01-09`. | HTTP 422; mensaje `La fecha límite debe ser posterior a la fecha de inicio`; no crea registro. | Postman: guardar cuerpo enviado y respuesta 422; evidencia de que el periodo no aparece al listar. |
| BE-09c | US-1.1 | `POST`; `fechaInicio` = ayer y `fechaLimite` = hoy + 10 días (fechas calculadas al ejecutar). | HTTP 422; mensaje `La fecha de inicio no puede ser anterior a hoy`; no crea registro. | Postman: guardar request/response 422 y captura del Runner. |
| BE-09d | US-1.1 | Crear periodo A: hoy + 5 a hoy + 15. Enviar periodo B: hoy + 10 a hoy + 20. | HTTP 409; mensaje identifica cruce con el periodo A; solo A queda registrado. | Postman: respuestas de ambos POST y listado que evidencia un solo periodo. |

La evidencia de cada ejecución debe incluir request, status y cuerpo de respuesta, sin contraseñas, JWT ni datos personales reales. Para los casos de interfaz, capturar el mensaje completo y el formulario sin credenciales.

## 3. Casos de interfaz

| ID | Historia / escenario | Pasos y resultado esperado | Resultado / evidencia |
| --- | --- | --- | --- |
| UI-01 | US-5.6 Inicio de sesión | Probar administrador válido, credenciales inválidas y visibilidad de contraseña. | **Parcial.** Administrador entró; la cuenta de alumno presentó confusión entre `username` y correo. El mensaje de rol sin pantalla disponible tras autenticación es esperado para el portal alumno actual. Adjuntar captura sin datos sensibles. |
| UI-02 | US-5.2 Acceso por rol | Entrar como administrador y como alumno; probar rutas administrativas y destino del alumno. | **Aprobado para el alcance actual.** Admin accede; alumno autenticado llega a acceso denegado para `/panel` porque su portal aún no está implementado. |
| UI-03 | US-1.2 Registro de estudiante | Como admin, registrar estudiante con usuario/contraseña, datos personales y apoderado; verificar alta en API/listado y validaciones. | **Aprobado según prueba manual reportada.** Adjuntar captura del formulario/listado y respuesta redactada. |
| UI-04 | US-1.2 Consulta de estudiantes | Abrir listado y comprobar datos cargados, estados de carga/vacío/error y filtros disponibles. | **Pendiente de evidencia específica del listado.** No confundir alta exitosa con verificación completa de búsqueda/paginación. |
| UI-05 | US-5.2 Gestión de usuarios | Editar cuenta de prueba, validar correo, desactivar/reactivar y revisar resultado en listado/login. | **Aprobado según prueba manual reportada.** Adjuntar capturas antes/después. |
| UI-06 | US-1.1 Periodo de matrícula | Crear/consultar una ventana vigente, pendiente o vencida; probar fechas inválidas. | **Aprobado según verificación manual previa y Postman.** Adjuntar capturas con fechas usadas. |
| UI-07 | US-1.7 Secciones y vacantes | Ver secciones y ocupación, crear sección/cambiar capacidad y comparar vacantes tras matrícula. | **Aprobado según prueba manual reportada.** Adjuntar capturas antes/después. |
| UI-08 | US-3.5 Catálogo curricular | Crear área/competencia, editar, activar/desactivar y recargar. | **Aprobado según prueba manual reportada.** Adjuntar capturas de persistencia. |
| UI-09 | US-4.3 Horarios | Crear/editar plantilla, revisar bloques y probar exceso de duración. | **Aprobado según prueba manual reportada.** Adjuntar capturas de caso válido e inválido. |
| UI-10 | US-1.2 Matrícula desde interfaz | Seleccionar estudiante, registrar matrícula y comprobar confirmación, duplicado y actualización de vacantes. | **Aprobado según prueba manual reportada.** La pantalla ya consumió datos del backend; no está basada en datos demo. Adjuntar captura del resultado y vacantes. |
| UI-11 | US-5.5 y US-5.6 Logs/recuperación | Revisar lista/detalle de logs; solicitar recuperación desde login y recibir correo real. | **Pendiente.** La prueba manual de logs no fue confirmada. Recuperación presenta la discrepancia de correo descrita en BE-07. |

## 4. Automatización conocida y limitaciones

- Backend: se ejecutó una selección de 59 pruebas de integración con resultado **59 aprobadas, 0 fallidas** antes de los últimos cambios de recuperación. Después se ejecutaron 6 pruebas de estudiante/periodo con **6 aprobadas**. Volver a correr recuperación tras el cambio de contrato.
- Postman: el equipo reportó que la colección pasó tras corregir fechas y datos; el último cambio de recuperación aún no tiene resultado confirmado. Guardar la salida del Runner de esa ejecución final.
- Frontend: 9 pruebas Vitest aprobadas y `vue-tsc --noEmit` aprobado. `nuxt build` llegó a Nitro y falló con `EPERM` al crear una carpeta; no se debe consignar como compilación aprobada.
- La recuperación real por correo necesita SMTP y buzón accesible. Las pruebas con `JavaMailSender` simulado validan lógica/token, no entrega real al buzón.
- La asignación de grado/sección ocurre en la matrícula. Registro del alumno y matrícula son flujos relacionados, pero distintos.

## 5. Registro de ejecución

| ID | Fecha/hora | Rol ejecutor | Entorno/build | Resultado | Evidencia a adjuntar / incidencia |
| --- | --- | --- | --- | --- | --- |
| BE-01–BE-15 (excepto BE-07 final) | 23/10/2026, según ejecución reportada | Backend / Integración / Testing | Backend local, MySQL y colección Postman; confirmar commit/build | Aprobado según reportes, con BE-07 pendiente | Exportar Runner final, ocultar tokens/correos reales; revalidar BE-07 |
| UI-02, UI-03, UI-05–UI-10 | 23/10/2026, según prueba reportada | Frontend / Integración | Frontend y backend locales; confirmar commit/build | Aprobado según reportes | Adjuntar capturas de cada flujo antes de entrega |
| UI-01, UI-04, UI-11 | Por completar | Frontend / Integración | Entorno local | Parcial/Pendiente | Validar identificador de login, listado, logs y recuperación |

