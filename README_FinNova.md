# FinNova

Aplicación web de gestión financiera personal y para pequeños negocios. Proyecto final integrador — Seminario de Integración Profesional (SIP), Ingeniería Informática Plan 22, Universidad del Salvador.

**Autor:** Uriel Agustín Olmedo
**Profesor:** Lic. Christian López Pasarón

## Stack tecnológico

- **Backend:** Java 17 + Spring Boot 3.3.x + Spring Security + JWT
- **Frontend:** React 18 + Vite
- **Base de datos:** PostgreSQL 18
- **Envío de mail:** Spring Mail (Gmail SMTP)

## Estructura del repositorio

```
finnova/
├── finnova-backend/    # API REST (Spring Boot)
├── finnova-frontend/   # Aplicación web (React + Vite)
└── README.md
```

## Requisitos previos

- JDK 17
- Node.js 20 LTS
- PostgreSQL (con una base `finnova_db` creada)
- Una cuenta de Gmail con verificación en 2 pasos activada y una [App Password](https://myaccount.google.com/apppasswords) generada (para el envío de mails de recuperación de contraseña)

## Cómo levantar el backend

```bash
cd finnova-backend
```

Configurar `src/main/resources/application.properties` con tus propios datos (este archivo **no se sube a Git**, ver `.gitignore`):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/finnova_db
spring.datasource.username=postgres
spring.datasource.password=TU_PASSWORD_DE_POSTGRES

spring.mail.username=TU_EMAIL@gmail.com
spring.mail.password=TU_APP_PASSWORD_DE_GOOGLE_SIN_ESPACIOS
```

Correr:
```bash
./mvnw spring-boot:run
```

Levanta en `http://localhost:8080`. Verificar con: `curl http://localhost:8080/api/ping` → debe responder `pong`.

## Cómo levantar el frontend

```bash
cd finnova-frontend
npm install
npm run dev
```

Levanta en `http://localhost:5173`.

## Módulo 1 — Autenticación y Perfil de Usuario (completo)

Endpoints disponibles bajo `/api/auth` y `/api/usuarios`:

| Método | Endpoint | CU | Descripción |
|---|---|---|---|
| POST | `/api/auth/registro` | CU-001 | Registrar nuevo usuario |
| POST | `/api/auth/login` | CU-002 | Iniciar sesión (devuelve JWT) |
| POST | `/api/auth/logout` | CU-003 | Cerrar sesión (invalida el token) |
| POST | `/api/auth/recuperar-password` | CU-004 | Solicitar recuperación (envía mail) |
| POST | `/api/auth/resetear-password` | CU-004 | Confirmar nueva contraseña con el token del mail |
| GET | `/api/usuarios/perfil` | — | Obtener perfil del usuario autenticado |
| PUT | `/api/usuarios/perfil` | CU-005 | Editar nombre/apellido |
| PUT | `/api/usuarios/modulos` | CU-006 | Configurar módulos activos |

Todos los endpoints de `/api/usuarios/**` requieren el header `Authorization: Bearer <token>`, obtenido del login o registro.

### Decisiones de diseño relevantes

- **JWT stateless con blacklist en memoria** para el logout: como el JWT no se puede "invalidar" del lado del servidor por defecto, se guarda una lista de tokens deslogueados en memoria (`TokenBlacklistService`). Limitación conocida: la lista se pierde si el backend reinicia. Para producción real convendría Redis con TTL igual a la expiración del token.
- **Passwords nunca en texto plano:** se guardan hasheadas con BCrypt (`PasswordEncoder`).
- **Recuperación de contraseña:** el endpoint `/recuperar-password` siempre responde el mismo mensaje genérico exista o no el email, para no filtrar qué emails están registrados en el sistema.
- **Token de recuperación:** UUID de un solo uso, expira a los 15 minutos (tabla `password_reset_tokens`).

## Módulo 2 — Registro de Ingresos y Egresos (completo)

Endpoints bajo `/api/transacciones` y `/api/categorias`, todos requieren `Authorization: Bearer <token>` salvo que se indique lo contrario:

| Método | Endpoint | CU | Descripción |
|---|---|---|---|
| GET | `/api/categorias` | — | Listar categorías disponibles (predefinidas) |
| POST | `/api/transacciones` | CU-007 / CU-008 | Registrar ingreso o egreso (el campo `tipo` define cuál). Si `esRecurrente=true`, genera automáticamente todas las instancias futuras hasta `fechaFinRecurrencia` |
| GET | `/api/transacciones` | CU-011 | Historial completo del usuario, ordenado por fecha descendente |
| GET | `/api/transacciones/filtrar/fecha?desde=&hasta=` | CU-012 | Filtrar por rango de fechas |
| GET | `/api/transacciones/filtrar/categoria/{id}` | CU-013 | Filtrar por categoría |
| PUT | `/api/transacciones/{id}` | CU-009 | Editar una transacción propia |
| DELETE | `/api/transacciones/{id}` | CU-010 | Eliminar una transacción propia |
| POST | `/api/transacciones/{id}/comprobante` | CU-014 | Adjuntar comprobante (multipart/form-data, campo `archivo`). JPG/PNG/PDF, máx. 5MB |

Los comprobantes quedan accesibles públicamente (sin JWT) en `http://localhost:8080/uploads/comprobantes/<archivo>` — el nombre es un UUID impredecible, usado como medida de protección suficiente para el alcance del proyecto.

### Decisiones de diseño relevantes

- **Categorías predefinidas:** como el Módulo 3 (categorías personalizadas) todavía no existe, se precargan 10 categorías fijas al arrancar el backend (`DataSeeder`), para no bloquear el registro de transacciones que las requiere como precondición.
- **Transacciones recurrentes (CU-015):** las instancias futuras se generan todas de una vez, en el momento de crear la transacción original (no con un job programado). Simplificación razonable para el alcance del proyecto.
- **BigDecimal para montos:** se usa `BigDecimal`, no `double`, para evitar errores de redondeo típicos del punto flotante al manejar dinero.
- **Seguridad por pertenencia:** editar/eliminar una transacción valida que pertenezca al usuario logueado (`existsByIdAndUsuarioId`), para que nadie pueda modificar transacciones ajenas adivinando el ID por la URL.

## Sprints y planificación

Ver el documento `SIP_2026__P22__SP__OLMEDO_Uriel.xlsx` (Sprint Planning) para el detalle completo del backlog, la planificación de los 6 sprints y el seguimiento sprint a sprint.

## Convenciones del proyecto

- Commits en español, formato: `Sprint N: descripción breve`.
- Cada sprint (a partir del 2) debe cerrar con un release funcional.
- Los archivos con credenciales (`application.properties` con datos reales) nunca se suben a Git — usar el archivo como plantilla y completar localmente.
