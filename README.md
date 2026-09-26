# NeedlOS Backend

ERP multi-tenant para sastrerias. Spring Boot 4.1.1 · Java 21 · PostgreSQL.

## Requisitos

- **JDK 21** (Eclipse Temurin). Debe estar activo al compilar/ejecutar.
- **Docker Desktop** (para la base de datos de desarrollo).
- Maven (incluido el wrapper `./mvnw`).

> **Importante sobre JAVA_HOME:** si tu `JAVA_HOME` apunta a Java 17, Maven fallara con
> `release version 21 not supported`. Deja `JAVA_HOME` apuntando al JDK 21, por ejemplo:
> `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.

## Levantar la base de datos

```bash
docker compose up -d      # PostgreSQL en localhost:5433 (5432 lo usa tu Postgres local)
```

## Ejecutar la aplicacion

```bash
mvn spring-boot:run
```

- API:      http://localhost:8080
- Swagger:  http://localhost:8080/swagger-ui.html

Al primer arranque se crea un usuario de demostracion:

| slug | email            | password   | rol          |
|------|------------------|------------|--------------|
| demo | admin@demo.com   | Admin123!  | SASTRE_ADMIN |

## Probar en Swagger

1. `POST /api/auth/login` con `{"slug":"demo","email":"admin@demo.com","password":"Admin123!"}`.
2. Copia el `accessToken`, pulsa **Authorize** (arriba a la derecha) y pegalo.
3. Ya puedes usar el resto de endpoints (clientes, tipos de prenda, ordenes).

## Comandos utiles

```bash
docker compose stop        # apaga la BD (conserva los datos)
docker compose down -v     # borra la BD por completo (empezar de cero)
mvn -DskipTests package     # compila el jar
```

## Arquitectura (resumen)

- **Multi-tenancy** automatica con Hibernate `@TenantId`: cada consulta se filtra por
  sastreria sin codigo manual. Aislamiento verificado.
- **Auth** JWT (access) + refresh token rotado y hasheado.
- **Soft-delete** con `@SoftDelete`, **auditoria** JPA (quien/cuando).
- **Flyway** dueno del esquema; Hibernate solo valida (`ddl-auto: validate`).
- Dinero en `BigDecimal` / `NUMERIC(12,2)`.
- Estructura por features: `clientes`, `ordenes`, `tipoprenda`, `security`, `tenant`, `common`.
