-- ═══════════════════════════════════════════════════════════════════
--  NeedlOS · Esquema inicial (multi-tenant)
-- ═══════════════════════════════════════════════════════════════════
--  Modelo de identidad:
--   · Cuenta  = identidad global de una persona (unica por correo).
--   · Acceso  = vinculo de una Cuenta con una Sastreria (tenant) + roles.
--     Una cuenta puede tener varios accesos (varias sastrerias).
--  Convenciones (Reglas §9, §10): PK UUID; dinero NUMERIC(12,2) con CHECK;
--  fechas-hora TIMESTAMPTZ (UTC); tablas de negocio con tenant_id NOT NULL,
--  auditoria, soft-delete y version (bloqueo optimista); indices que
--  empiezan por tenant_id; toda FK con indice.
--
--  Esta migracion solo se edita hasta el primer despliegue compartido.
--  Desde ese momento cada cambio es un V<n>__descripcion.sql nuevo.
--  Los datos de prueba NO van aqui: los crea DatosPruebaInitializer (perfil dev).
-- ═══════════════════════════════════════════════════════════════════

-- ── Sastrerias (tenants) ────────────────────────────────────────────
CREATE TABLE tenants (
    id             UUID         PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    slug           VARCHAR(60)  NOT NULL UNIQUE,
    plan           VARCHAR(20)  NOT NULL DEFAULT 'DEMO',
    licencia_hasta TIMESTAMPTZ,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_tenants_plan CHECK (plan IN ('DEMO', 'LICENCIADO'))
);

-- ── Roles (globales) ────────────────────────────────────────────────
CREATE TABLE roles (
    id     UUID        PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL UNIQUE
);

-- ── Cuentas (identidad global de una persona) ───────────────────────
CREATE TABLE cuentas (
    id               UUID         PRIMARY KEY,
    email            VARCHAR(120) NOT NULL UNIQUE,
    password_hash    VARCHAR(100),          -- null si es cuenta solo-Google
    google_sub       VARCHAR(64) UNIQUE,    -- id de Google (null si no vinculada)
    nombre           VARCHAR(60)  NOT NULL,
    apellido         VARCHAR(60)  NOT NULL,
    numero_documento VARCHAR(30),
    telefono         VARCHAR(20),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    super_admin      BOOLEAN      NOT NULL DEFAULT FALSE,
    -- TRUE por defecto: solo el auto-registro con correo+contrasena exige verificar
    -- (Google ya verifica el correo; las demas cuentas, p. ej. empleados, no lo necesitan).
    verificada       BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── Accesos (Cuenta ↔ Sastreria) ────────────────────────────────────
CREATE TABLE accesos (
    id         UUID        PRIMARY KEY,
    cuenta_id  UUID        NOT NULL REFERENCES cuentas(id) ON DELETE CASCADE,
    tenant_id  UUID        NOT NULL REFERENCES tenants(id),
    activo     BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_acceso_cuenta_tenant UNIQUE (cuenta_id, tenant_id)
);
CREATE INDEX ix_accesos_tenant ON accesos(tenant_id);

-- ── Roles de cada acceso ────────────────────────────────────────────
CREATE TABLE acceso_roles (
    acceso_id UUID NOT NULL REFERENCES accesos(id) ON DELETE CASCADE,
    rol_id    UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (acceso_id, rol_id)
);
CREATE INDEX ix_acceso_roles_rol ON acceso_roles(rol_id);

-- ── Sesiones (una por dispositivo/navegador) ────────────────────────
--  El refresh token se guarda HASHEADO (SHA-256); el valor en claro solo
--  vive en la cookie HttpOnly del navegador. token_hash_anterior permite
--  detectar el reuso de un refresh ya rotado (posible robo).
CREATE TABLE sesiones (
    id                  UUID         PRIMARY KEY,   -- = sid dentro del access token
    cuenta_id           UUID         NOT NULL REFERENCES cuentas(id) ON DELETE CASCADE,
    tenant_id           UUID         REFERENCES tenants(id),  -- null para SUPER_ADMIN
    token_hash          VARCHAR(64)  NOT NULL UNIQUE,
    token_hash_anterior VARCHAR(64),
    rotada_en           TIMESTAMPTZ,
    device_info         VARCHAR(60),
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(255),
    expira              TIMESTAMPTZ  NOT NULL,
    revocada            BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ultimo_uso          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_sesiones_cuenta          ON sesiones(cuenta_id);
CREATE INDEX ix_sesiones_tenant          ON sesiones(tenant_id);
CREATE INDEX ix_sesiones_token_anterior  ON sesiones(token_hash_anterior);

-- ── Recuperacion de contrasena ──────────────────────────────────────
--  Enlace de un solo uso enviado por correo. Solo se guarda el hash del
--  token; al pedir uno nuevo, los anteriores sin usar se eliminan.
CREATE TABLE recuperaciones_contrasena (
    id          UUID        PRIMARY KEY,
    cuenta_id   UUID        NOT NULL REFERENCES cuentas(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expira      TIMESTAMPTZ NOT NULL,
    usado_en    TIMESTAMPTZ,
    ip_address  VARCHAR(45),
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_recuperaciones_cuenta ON recuperaciones_contrasena(cuenta_id);

-- ── Verificacion de correo (registro con correo + contrasena) ───────
--  Codigo de 6 digitos de un solo uso. Solo se guarda el hash; a diferencia
--  de los tokens largos (recuperacion, refresh), un codigo de 6 digitos SI es
--  adivinable por fuerza bruta, por eso lleva un contador de intentos fallidos
--  (ver VerificacionCorreoService): agotados, el codigo se invalida y hay que
--  pedir uno nuevo.
CREATE TABLE verificaciones_correo (
    id                UUID        PRIMARY KEY,
    cuenta_id         UUID        NOT NULL REFERENCES cuentas(id) ON DELETE CASCADE,
    codigo_hash       VARCHAR(64) NOT NULL,
    intentos_fallidos INTEGER     NOT NULL DEFAULT 0,
    expira            TIMESTAMPTZ NOT NULL,
    creado_en         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_verificaciones_cuenta ON verificaciones_correo(cuenta_id);

-- ── Consecutivos por sastreria (Reglas §10.4: nunca max + 1) ────────
CREATE TABLE consecutivos (
    tenant_id UUID        NOT NULL REFERENCES tenants(id),
    tipo      VARCHAR(30) NOT NULL,
    ultimo    BIGINT      NOT NULL,
    PRIMARY KEY (tenant_id, tipo),
    CONSTRAINT ck_consecutivos_ultimo CHECK (ultimo > 0)
);

-- ── Clientes ────────────────────────────────────────────────────────
CREATE TABLE clientes (
    id              UUID         PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    nombre          VARCHAR(60)  NOT NULL,
    apellido        VARCHAR(60)  NOT NULL,
    telefono        VARCHAR(20),
    fecha_registro  DATE         NOT NULL DEFAULT CURRENT_DATE,
    eliminado       BOOLEAN      NOT NULL DEFAULT FALSE,
    version         BIGINT       NOT NULL DEFAULT 0,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ,
    creado_por      UUID,
    actualizado_por UUID
);
CREATE INDEX ix_clientes_tenant_apellido ON clientes(tenant_id, apellido, nombre);

-- ── Tipos de prenda ─────────────────────────────────────────────────
CREATE TABLE tipos_prenda (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL REFERENCES tenants(id),
    nombre          VARCHAR(60)   NOT NULL,
    precio_base     NUMERIC(12,2),
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    eliminado       BOOLEAN       NOT NULL DEFAULT FALSE,
    version         BIGINT        NOT NULL DEFAULT 0,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ,
    creado_por      UUID,
    actualizado_por UUID,
    CONSTRAINT ck_tipos_prenda_precio CHECK (precio_base IS NULL OR precio_base >= 0)
);
CREATE INDEX ix_tipos_prenda_tenant_nombre ON tipos_prenda(tenant_id, nombre);

-- ── Pedidos (en el codigo aun "ordenes"; se renombra en la Fase 4) ──
CREATE TABLE ordenes (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL REFERENCES tenants(id),
    numero          BIGINT        NOT NULL,
    cliente_id      UUID          NOT NULL REFERENCES clientes(id),
    fecha           DATE          NOT NULL DEFAULT CURRENT_DATE,
    fecha_entrega   DATE          NOT NULL,
    descuento       NUMERIC(12,2) NOT NULL DEFAULT 0,
    anulada         BOOLEAN       NOT NULL DEFAULT FALSE,
    razon_anulacion VARCHAR(300),
    eliminado       BOOLEAN       NOT NULL DEFAULT FALSE,
    version         BIGINT        NOT NULL DEFAULT 0,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ,
    creado_por      UUID,
    actualizado_por UUID,
    CONSTRAINT uq_orden_numero_por_tenant UNIQUE (tenant_id, numero),
    CONSTRAINT ck_ordenes_descuento CHECK (descuento >= 0),
    CONSTRAINT ck_ordenes_anulacion CHECK (NOT anulada OR razon_anulacion IS NOT NULL)
);
CREATE INDEX ix_ordenes_tenant_cliente ON ordenes(tenant_id, cliente_id);
CREATE INDEX ix_ordenes_tenant_entrega ON ordenes(tenant_id, fecha_entrega);

-- ── Prendas (lineas del pedido) ─────────────────────────────────────
CREATE TABLE prendas (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL REFERENCES tenants(id),
    orden_id        UUID          NOT NULL REFERENCES ordenes(id),
    tipo_prenda_id  UUID          NOT NULL REFERENCES tipos_prenda(id),
    sastre_id       UUID          REFERENCES accesos(id),
    cantidad        INT           NOT NULL,
    descripcion     VARCHAR(200)  NOT NULL,
    precio_unitario NUMERIC(12,2) NOT NULL,
    estado          VARCHAR(20)   NOT NULL,
    eliminado       BOOLEAN       NOT NULL DEFAULT FALSE,
    version         BIGINT        NOT NULL DEFAULT 0,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ,
    creado_por      UUID,
    actualizado_por UUID,
    CONSTRAINT ck_prendas_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_prendas_precio   CHECK (precio_unitario >= 0),
    CONSTRAINT ck_prendas_estado   CHECK (estado IN ('EN_PROCESO', 'FINALIZADO', 'ENTREGADO'))
);
CREATE INDEX ix_prendas_orden       ON prendas(orden_id);
CREATE INDEX ix_prendas_tenant_tipo ON prendas(tenant_id, tipo_prenda_id);
CREATE INDEX ix_prendas_sastre      ON prendas(sastre_id);

-- ── Historial de estados de una prenda ──────────────────────────────
CREATE TABLE estado_prenda_historial (
    id           UUID        PRIMARY KEY,
    tenant_id    UUID        NOT NULL REFERENCES tenants(id),
    prenda_id    UUID        NOT NULL REFERENCES prendas(id) ON DELETE CASCADE,
    estado       VARCHAR(20) NOT NULL,
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id   UUID,
    CONSTRAINT ck_estado_hist_estado CHECK (estado IN ('EN_PROCESO', 'FINALIZADO', 'ENTREGADO'))
);
CREATE INDEX ix_estado_hist_prenda ON estado_prenda_historial(prenda_id);

-- ═══════════════════════════════════════════════════════════════════
--  Datos de referencia (necesarios en todos los ambientes)
-- ═══════════════════════════════════════════════════════════════════

-- Roles de acceso (SUPER_ADMIN vive en la cuenta, no como rol de acceso).
INSERT INTO roles (id, nombre) VALUES
    ('00000000-0000-0000-0000-000000000010', 'SASTRE'),
    ('00000000-0000-0000-0000-000000000011', 'SASTRE_ADMIN');
