-- ═══════════════════════════════════════════════════════════════════
--  NeedlOS · Esquema inicial (multi-tenant)
-- ═══════════════════════════════════════════════════════════════════
--  Convenciones:
--   · Todas las PK son UUID.
--   · Las tablas de negocio llevan tenant_id + columnas de auditoria
--     y soft-delete (eliminado). El aislamiento por tenant lo aplica
--     Hibernate automaticamente via @TenantId.
--   · El dinero se guarda como NUMERIC(12,2), nunca como float.
-- ═══════════════════════════════════════════════════════════════════

-- ── Tenants (las sastrerias que usan el sistema) ────────────────────
CREATE TABLE tenants (
    id             UUID         PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    slug           VARCHAR(60)  NOT NULL UNIQUE,
    plan           VARCHAR(20)  NOT NULL DEFAULT 'DEMO',
    licencia_hasta TIMESTAMPTZ,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── Roles (globales, compartidos por todos los tenants) ─────────────
CREATE TABLE roles (
    id     UUID        PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL UNIQUE
);

-- ── Usuarios (empleados de una sastreria) ───────────────────────────
CREATE TABLE usuarios (
    id               UUID         PRIMARY KEY,
    tenant_id        UUID         NOT NULL REFERENCES tenants(id),
    nombre           VARCHAR(60)  NOT NULL,
    apellido         VARCHAR(60)  NOT NULL,
    numero_documento VARCHAR(30)  NOT NULL,
    email            VARCHAR(120) NOT NULL,
    password_hash    VARCHAR(100) NOT NULL,
    telefono         VARCHAR(20),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuario_email_por_tenant UNIQUE (tenant_id, email)
);
CREATE INDEX ix_usuarios_tenant ON usuarios(tenant_id);

-- ── Relacion usuario ↔ rol (muchos a muchos) ────────────────────────
CREATE TABLE usuario_roles (
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id     UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (usuario_id, rol_id)
);

-- ── Refresh tokens (rotacion de sesion, hasheados) ──────────────────
CREATE TABLE refresh_tokens (
    id         UUID         PRIMARY KEY,
    usuario_id UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash VARCHAR(100) NOT NULL,
    expira     TIMESTAMPTZ  NOT NULL,
    revocado   BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_usuario ON refresh_tokens(usuario_id);

-- ── Clientes ────────────────────────────────────────────────────────
CREATE TABLE clientes (
    id              UUID         PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    nombre          VARCHAR(60)  NOT NULL,
    apellido        VARCHAR(60)  NOT NULL,
    telefono        VARCHAR(20),
    fecha_registro  DATE         NOT NULL DEFAULT CURRENT_DATE,
    eliminado       BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ,
    creado_por      UUID,
    actualizado_por UUID
);
CREATE INDEX ix_clientes_tenant ON clientes(tenant_id);

-- ── Tipos de prenda (catalogo por nombre, precio configurable) ──────
CREATE TABLE tipos_prenda (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL,
    nombre          VARCHAR(60)   NOT NULL,
    precio_base     NUMERIC(12,2),
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    eliminado       BOOLEAN       NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ,
    creado_por      UUID,
    actualizado_por UUID
);
CREATE INDEX ix_tipos_prenda_tenant ON tipos_prenda(tenant_id);

-- ── Ordenes de trabajo ──────────────────────────────────────────────
CREATE TABLE ordenes (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL,
    numero          BIGINT        NOT NULL,
    cliente_id      UUID          NOT NULL REFERENCES clientes(id),
    fecha           DATE          NOT NULL DEFAULT CURRENT_DATE,
    fecha_entrega   DATE          NOT NULL,
    descuento       NUMERIC(12,2) NOT NULL DEFAULT 0,
    anulada         BOOLEAN       NOT NULL DEFAULT FALSE,
    razon_anulacion VARCHAR(300),
    eliminado       BOOLEAN       NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ,
    creado_por      UUID,
    actualizado_por UUID,
    CONSTRAINT uq_orden_numero_por_tenant UNIQUE (tenant_id, numero)
);
CREATE INDEX ix_ordenes_tenant  ON ordenes(tenant_id);
CREATE INDEX ix_ordenes_cliente ON ordenes(cliente_id);

-- ── Prendas (lineas de la orden) ────────────────────────────────────
CREATE TABLE prendas (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL,
    orden_id        UUID          NOT NULL REFERENCES ordenes(id),
    tipo_prenda_id  UUID          NOT NULL REFERENCES tipos_prenda(id),
    sastre_id       UUID          REFERENCES usuarios(id),
    cantidad        INT           NOT NULL,
    descripcion     VARCHAR(200)  NOT NULL,
    precio_unitario NUMERIC(12,2) NOT NULL,
    estado          VARCHAR(20)   NOT NULL,
    eliminado       BOOLEAN       NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ,
    creado_por      UUID,
    actualizado_por UUID
);
CREATE INDEX ix_prendas_tenant ON prendas(tenant_id);
CREATE INDEX ix_prendas_orden  ON prendas(orden_id);

-- ── Historial de estados de una prenda (trazabilidad) ───────────────
CREATE TABLE estado_prenda_historial (
    id           UUID        PRIMARY KEY,
    tenant_id    UUID        NOT NULL,
    prenda_id    UUID        NOT NULL REFERENCES prendas(id) ON DELETE CASCADE,
    estado       VARCHAR(20) NOT NULL,
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id   UUID
);
CREATE INDEX ix_estado_hist_prenda ON estado_prenda_historial(prenda_id);

-- ═══════════════════════════════════════════════════════════════════
--  Datos semilla
-- ═══════════════════════════════════════════════════════════════════

-- Tenant de demostracion (plan DEMO).
INSERT INTO tenants (id, nombre, slug, plan) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Sastreria Demo', 'demo', 'DEMO');

-- Roles del sistema.
INSERT INTO roles (id, nombre) VALUES
    ('00000000-0000-0000-0000-000000000010', 'SASTRE'),
    ('00000000-0000-0000-0000-000000000011', 'SASTRE_ADMIN'),
    ('00000000-0000-0000-0000-000000000012', 'SUPER_ADMIN');

-- Catalogo inicial de tipos de prenda para el tenant demo (precios de ejemplo).
INSERT INTO tipos_prenda (id, tenant_id, nombre, precio_base) VALUES
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Camisa',    18000.00),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Pantalon',  12000.00),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Saco',      15000.00),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Vestido',   12000.00),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Chaqueta',  15000.00);

-- El usuario dueno (SASTRE_ADMIN) se crea al arrancar (SeedDataInitializer)
-- para hashear la contrasena con BCrypt de forma segura.
