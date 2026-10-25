# Informe de resultados del Plan de Capacitación

**Paquete:** 5.7 · **Periodo informado:** 28/08/2026–23/10/2026 · **Equipo:** EducaShow  
**Criterio de reporte:** avances descritos por rol, sin nombres. Los estados se basan en artefactos y actividades técnicas observables del proyecto; fechas y participación individual deben confirmarse con el equipo antes de presentar.

## 1. Resumen

Durante la Iteración 1 se aplicaron conocimientos de Git/GitHub, MySQL, Flyway, Spring Boot, Spring Security, Nuxt/TypeScript, pruebas e integración frontend-backend. La evidencia incluye migraciones ejecutadas en pruebas, APIs protegidas por rol, formularios y vistas conectados al backend, pruebas automatizadas y resolución colaborativa de cambios. Testing frontend y el prototipo/despliegue AWS requieren más evidencia para considerarse concluidos. La recuperación por correo y el inicio de sesión con identificadores distintos a correo siguen presentando una inconsistencia de integración que debe retomarse en la siguiente iteración.

## 2. Resultados por actividad

| ID | Capacitación | Resultado observado por rol | Estado | Evidencia / siguiente paso |
| --- | --- | --- | --- | --- |
| CAP-01 | Configuración del entorno | **Todo el equipo:** configuración local de backend, frontend y base para probar los flujos. | Completada | Backend y frontend iniciados localmente; registrar versión/fecha exacta del entorno empleado. |
| CAP-02 | Git, GitHub y flujo colaborativo | **Todo el equipo:** trabajo con ramas, commits, PR y resolución de conflictos durante la integración. | Completada | Historial de commits/PR y conflictos resueltos. Adjuntar enlaces o capturas del repositorio. |
| CAP-03 | MySQL y modelado relacional | **Backend / Datos:** uso del esquema relacional para personas, estudiantes, cuentas, matrícula y vínculos familiares. | Completada | Migraciones Flyway y modelo usado por las pruebas. |
| CAP-04 | Flyway | **Backend / Datos:** aplicación y comprobación de migraciones, incluidas las migraciones de la rama usadas en H2. | Completada | Logs de Flyway que muestran validación/aplicación; para demo, adjuntar salida redactada. |
| CAP-05 | Spring Boot + Spring Data JPA | **Backend:** implementación de controladores, servicios, repositorios y transacciones para las historias del sprint. | Completada | Código del backend, `spotless:check` y empaquetado aprobados; pruebas de integración. |
| CAP-06 | Nuxt 3 + TypeScript | **Frontend:** páginas/composables tipados, validación de formularios y estados de carga/error. | Completada | `vue-tsc --noEmit` aprobado y 9 pruebas Vitest aprobadas. No declarar Nuxt build aprobada: falló por `EPERM` en Nitro. |
| CAP-07 | Spring Security | **Backend / Seguridad:** autenticación, JWT y restricción de operaciones administrativas por rol. | Completada | Pruebas de autenticación/autorización y verificación manual de admin/alumno. |
| CAP-08 | Consumo de APIs desde Nuxt | **Frontend:** integración de usuarios, estudiantes, matrícula, vacantes, catálogo, horarios y logs con el backend. | Completada con observaciones | Flujos manuales reportados satisfactorios; pendiente corregir/verificar contrato de login/recuperación cuando username y correo difieren. |
| CAP-09 | Testing backend | **Backend / Testing:** pruebas unitarias/de integración con JUnit, Spring Test y Mockito; ejecución de colección Postman. | Completada | 59 pruebas seleccionadas aprobadas y luego 6 pruebas focalizadas aprobadas antes del último cambio de recuperación; reejecutar recuperación. |
| CAP-10 | Testing frontend | **Frontend / Testing:** ejecución de pruebas Vitest y comprobación estática de tipos. | En curso | 9 pruebas Vitest y `vue-tsc` aprobados. Ampliar cobertura de páginas/flows; documentar el fallo de `nuxt build` por permisos si se incluye. |
| CAP-11 | Integración frontend-backend | **Frontend + Backend:** prueba manual del recorrido desde UI hacia API y base de datos para usuarios, estudiantes, matrícula, catálogo y horarios. | Completada con observaciones | Reportes de prueba manual y Postman; completar recuperación/login e incorporar capturas de evidencia. |
| CAP-12 | AWS y despliegue | **DevOps / Integración:** el artefacto disponible no acredita un laboratorio ni despliegue AWS completado durante la Iteración 1. | Pendiente | Realizar spike de despliegue y conservar configuración/URL/logs de entorno de prueba antes de marcar como completada. |

## 3. Avance por rol

| Rol | Avance observado | Necesidad de refuerzo |
| --- | --- | --- |
| Backend | CRUD/API, JPA, Flyway, transacciones, validación, seguridad y pruebas de integración aplicadas en las historias del sprint. | Reconciliar el contrato de autenticación/recuperación y volver a probar el flujo SMTP extremo a extremo. |
| Frontend | Nuxt/Vue/TypeScript y consumo de API aplicados a los flujos principales; se validaron estados de carga, éxito y error. | Completar tests de interfaz, revisar responsive/accesibilidad básica y corregir el identificador mostrado en login/recuperación. |
| Testing | Pruebas API con Postman y suites Java; Vitest y comprobación de tipos en frontend. | Adjuntar exportación de resultados y cubrir regresiones de recuperación/login tras el cambio de contrato. |
| DevOps / Integración | Configuración local y coordinación entre repositorios para levantar la aplicación completa. | Falta evidencia verificable de despliegue AWS; mantener esta actividad pendiente. |
| Planning/Design | Coordinación del alcance por historias y acuerdos de integración entre roles. | Conservar contratos API y decisiones de modelo actualizados en la documentación compartida. |
| Todo el equipo | Uso práctico del repositorio, revisión de cambios e integración de funcionalidades. | Confirmar en retrospectiva qué roles participaron en cada actividad y añadir las fechas reales de sesiones si se requiere asistencia formal. |

## 4. Evidencias que se deben anexar

1. Salida de `mvnw.cmd verify` o reportes Surefire del commit evaluado; distinguir pruebas ejecutadas antes/después de cambios recientes.
2. Resumen del Runner Postman de la ejecución final, incluyendo códigos/assertions; redactar contraseñas, JWT, correos personales y tokens.
3. Capturas de las pantallas de administración que el equipo probó manualmente y de sus resultados persistidos.
4. Salida de Vitest y `vue-tsc`; registrar Nuxt build como no aprobada hasta repetirla en un entorno sin el error `EPERM`.
5. Enlaces a PR/commits que evidencien Git, integración y colaboración.
6. Evidencia AWS únicamente cuando se complete el laboratorio (no inferir un despliegue por tener AWS en el plan).

## 5. Conclusión

La capacitación práctica de roles Backend, Frontend, Testing y de integración se aplicó al desarrollo y verificación de la Iteración 1. La evidencia disponible respalda avances en Git, Spring, Nuxt, Flyway, seguridad, APIs y automatización. Permanecen como trabajo de capacitación la cobertura de pruebas UI, el despliegue AWS y la consistencia del identificador de login/recuperación cuando el usuario y su correo no coinciden.

