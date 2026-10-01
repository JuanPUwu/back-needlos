# NeedlOS Backend

ERP multi-tenant para sastrerias. Spring Boot 4.1.1 · Java 21 · PostgreSQL 16.

Reglas de construccion: `../Reglas.MD` · Alcance por fases: `../Manual.MD`.

## Requisitos

- **JDK 21** (Eclipse Temurin). Si `JAVA_HOME` apunta a Java 17, Maven falla con
  `release version 21 not supported`. Ejemplo: `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.
- **Docker Desktop** (base de datos de desarrollo y pruebas con Testcontainers).
- Maven (incluido el wrapper `./mvnw`).

## Perfiles

| Perfil | Uso | Como se activa |
|---|---|---|
| `dev`  | Desarrollo local: datos de prueba, Swagger, cookie sin `Secure` | **Por defecto** (`spring.profiles.default` en `application.yaml`): no hay que configurar nada |
| `prod` | Produccion: sin datos de prueba ni Swagger, logs JSON, cabeceras de seguridad | `SPRING_PROFILES_ACTIVE=prod` |
| `test` | Pruebas automaticas (lo activan las propias pruebas) | — |

**Produccion SIEMPRE** con `SPRING_PROFILES_ACTIVE=prod` (variable de entorno del servicio). Sin ella arrancaria en `dev`,
con las cuentas de prueba. En `prod` los secretos no tienen valor por defecto (fail fast, Reglas §8.4).

### Secretos locales: archivo `.env`

Copia `.env.example` como `.env` (misma carpeta que `pom.xml`) y completa los valores. `.env` esta en
`.gitignore`: nunca se sube ni se comparte. Spring lo lee solo al arrancar.

Para que salgan los correos (recuperacion de contrasena, etc.) hace falta `CLOUDFLARE_EMAIL_TOKEN`: un token
de la API de Cloudflare (Mi perfil → API Tokens → Create Token, con permiso de envio de Email), mas
`CLOUDFLARE_ACCOUNT_ID` (se ve en la URL del dashboard). Se envian con la API de envio de Cloudflare Email,
sobre el dominio `needlos.com` ya verificado ahi; sin esos dos valores la app funciona pero no envia correos
(lo advierte en el log).

### Variables de entorno (obligatorias en `prod`)

| Variable | Descripcion |
|---|---|
| `DB_URL` | p. ej. `jdbc:postgresql://localhost:5433/needlos` |
| `DB_USER` / `DB_PASSWORD` | usuario de BD con minimo privilegio |
| `JWT_SECRET` | clave aleatoria de al menos 32 caracteres (`openssl rand -base64 48`) |
| `FRONTEND_URLS` | origenes permitidos separados por coma, p. ej. `https://needlos.com` |
| `APP_URL` | direccion publica del frontend para los enlaces de los correos, p. ej. `https://needlos.com` |
| `CLOUDFLARE_ACCOUNT_ID` / `CLOUDFLARE_EMAIL_TOKEN` | cuenta y token de la API de envio de Cloudflare Email |
| `SERVER_PORT` | opcional (8080) |

## Levantar la base de datos

```bash
# Siempre dentro de la carpeta needlos-backend (ahi esta docker-compose.yml)
docker compose up -d      # PostgreSQL en localhost:5433 (5432 lo usa tu Postgres local)
```

> Mientras no exista un despliegue compartido, `V1__esquema_inicial.sql` puede cambiar.
> Si cambia, recrea la BD local: `docker compose down -v && docker compose up -d`.

## Ejecutar

```bash
mvn spring-boot:run       # perfil dev (por defecto)
```

- API: http://localhost:8080/api/v1/...
- Swagger (solo dev): http://localhost:8080/swagger-ui.html

### Cuentas de prueba (solo perfil dev, contrasena `Test123!`)

| Correo | Rol |
|---|---|
| `pablys8@gmail.com` | SUPER_ADMIN (solo Google) |
| `admin@example.com` | SASTRE_ADMIN de SastreriaPablo y SastreriaAngely |
| `sastre@example.com` | SASTRE en ambas |

## Probar

```bash
mvn verify                # pruebas unitarias, de integracion (Testcontainers) y de arquitectura
```

Con Swagger: `POST /api/v1/auth/login`, copia `sesion.accessToken`, pulsa **Authorize**.
El refresh token viaja solo en la cookie HttpOnly `needlos_refresh`; `POST /api/v1/auth/refresh`
exige la cabecera `Origin` de un origen permitido.

## Arquitectura (resumen)

- Estructura **por funcionalidad** (`security`, `tenant`, `clientes`, `ordenes`, `tipoprenda`)
  con capas internas; `common` es transversal; `dev` contiene solo los datos de prueba.
- **Multi-tenancy** automatica con Hibernate `@TenantId`; el tenant sale siempre del token.
- **Sesiones** en BD: access token de 15 min con `sid`, refresh rotado y hasheado en cookie
  HttpOnly, deteccion de reuso, revocacion inmediata.
- **Errores** RFC 9457 (`ProblemDetail`) con `code`; id de correlacion `X-Request-Id`.
- **Flyway** dueno del esquema; Hibernate solo valida. Bloqueo optimista (`version`).
- Consecutivos por sastreria con contador atomico (nunca `max + 1`).
