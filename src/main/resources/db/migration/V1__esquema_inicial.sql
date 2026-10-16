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
    -- Datos opcionales del negocio para el ticket/factura (el SASTRE_ADMIN los llena si quiere).
    nit            VARCHAR(30),
    direccion      VARCHAR(200),
    telefono       VARCHAR(20),
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

-- ── Permisos de cada acceso ─────────────────────────────────────────
--  Acciones que el SASTRE_ADMIN otorga a cada miembro (el SASTRE_ADMIN los
--  tiene todos siempre). Un SASTRE nuevo no tiene ninguno.
CREATE TABLE acceso_permisos (
    acceso_id UUID        NOT NULL REFERENCES accesos(id) ON DELETE CASCADE,
    permiso   VARCHAR(40) NOT NULL,
    PRIMARY KEY (acceso_id, permiso),
    CONSTRAINT ck_acceso_permisos CHECK (permiso IN (
        'GESTIONAR_CLIENTES', 'CREAR_PEDIDOS', 'CAMBIAR_ESTADO_PRENDAS',
        'ENTREGAR_PRENDAS', 'ASIGNAR_SASTRE', 'REGISTRAR_ABONOS'))
);

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

-- ── Invitaciones de empleados ───────────────────────────────────────
--  El SASTRE_ADMIN invita por correo. No se crea la cuenta ni el acceso
--  hasta que la persona ACEPTA la invitacion (enlace de un solo uso; solo
--  se guarda el hash del token). Guarda los datos y el rol para crear el
--  acceso en el momento de aceptar.
CREATE TABLE invitaciones (
    id                UUID         PRIMARY KEY,
    tenant_id         UUID         NOT NULL REFERENCES tenants(id),
    email             VARCHAR(120) NOT NULL,
    nombre            VARCHAR(60)  NOT NULL,
    apellido          VARCHAR(60)  NOT NULL,
    telefono          VARCHAR(20),
    numero_documento  VARCHAR(30),
    rol               VARCHAR(40)  NOT NULL,
    token_hash        VARCHAR(64)  NOT NULL UNIQUE,
    expira            TIMESTAMPTZ  NOT NULL,
    usado_en          TIMESTAMPTZ,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_invitaciones_tenant ON invitaciones(tenant_id);
CREATE INDEX ix_invitaciones_email ON invitaciones(email);

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
    telefono        VARCHAR(20)  NOT NULL,
    fecha_registro  DATE         NOT NULL DEFAULT CURRENT_DATE,
    eliminado       BOOLEAN      NOT NULL DEFAULT FALSE,
    version         BIGINT       NOT NULL DEFAULT 0,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ,
    creado_por      UUID,
    actualizado_por UUID
);
CREATE INDEX ix_clientes_tenant_apellido ON clientes(tenant_id, apellido, nombre);
-- Telefono unico por sastreria entre clientes vigentes (al eliminar uno, su numero queda libre).
CREATE UNIQUE INDEX uq_clientes_tenant_telefono ON clientes(tenant_id, telefono) WHERE eliminado = FALSE;

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
-- Nombre unico por sastreria (sin distinguir mayusculas), ignorando los eliminados.
CREATE UNIQUE INDEX uq_tipos_prenda_tenant_nombre
    ON tipos_prenda(tenant_id, lower(nombre)) WHERE eliminado = FALSE;

-- ── Pedidos ─────────────────────────────────────────────────────────
--  Numero visible "#G-L": G = numero_global (unico entre TODAS las sastrerias,
--  secuencia de PostgreSQL; puede tener huecos si una creacion falla) y
--  L = numero (consecutivo de la sastreria, sin huecos: tabla consecutivos).
CREATE SEQUENCE pedidos_numero_global_seq START 1;

CREATE TABLE pedidos (
    id                 UUID          PRIMARY KEY,
    tenant_id          UUID          NOT NULL REFERENCES tenants(id),
    numero_global      BIGINT        NOT NULL UNIQUE,
    numero             BIGINT        NOT NULL,
    cliente_id         UUID          NOT NULL REFERENCES clientes(id),
    fecha              DATE          NOT NULL,
    fecha_entrega      DATE          NOT NULL,
    descuento          NUMERIC(12,2) NOT NULL DEFAULT 0,
    anulado            BOOLEAN       NOT NULL DEFAULT FALSE,
    razon_anulacion    VARCHAR(300),
    -- Protege contra el doble envio (cabecera Idempotency-Key, Reglas §5.8).
    clave_idempotencia UUID          NOT NULL,
    eliminado          BOOLEAN       NOT NULL DEFAULT FALSE,
    version            BIGINT        NOT NULL DEFAULT 0,
    creado_en          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ,
    creado_por         UUID,
    actualizado_por    UUID,
    CONSTRAINT uq_pedidos_numero_por_tenant UNIQUE (tenant_id, numero),
    CONSTRAINT uq_pedidos_idempotencia UNIQUE (tenant_id, clave_idempotencia),
    CONSTRAINT ck_pedidos_descuento CHECK (descuento >= 0),
    CONSTRAINT ck_pedidos_anulacion CHECK (NOT anulado OR razon_anulacion IS NOT NULL)
);
CREATE INDEX ix_pedidos_tenant_cliente ON pedidos(tenant_id, cliente_id);
CREATE INDEX ix_pedidos_tenant_entrega ON pedidos(tenant_id, fecha_entrega);

-- ── Prendas (lineas del pedido) ─────────────────────────────────────
CREATE TABLE prendas (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL REFERENCES tenants(id),
    pedido_id       UUID          NOT NULL REFERENCES pedidos(id),
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
CREATE INDEX ix_prendas_pedido      ON prendas(pedido_id);
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

-- ── Abonos (pagos de un pedido) ─────────────────────────────────────
--  Nunca se borran: un abono se ANULA (solo SASTRE_ADMIN, con motivo y razon).
--  Motivo DEVOLUCION = el dinero se le regreso al cliente (cuenta para caja, F6);
--  ERROR_REGISTRO = el abono se registro por error (el dinero nunca entro).
CREATE TABLE abonos (
    id                 UUID          PRIMARY KEY,
    tenant_id          UUID          NOT NULL REFERENCES tenants(id),
    pedido_id          UUID          NOT NULL REFERENCES pedidos(id),
    monto              NUMERIC(12,2) NOT NULL,
    metodo             VARCHAR(20)   NOT NULL,
    anulado            BOOLEAN       NOT NULL DEFAULT FALSE,
    motivo_anulacion   VARCHAR(20),
    razon_anulacion    VARCHAR(300),
    anulado_en         TIMESTAMPTZ,
    anulado_por        UUID,
    clave_idempotencia UUID          NOT NULL,
    version            BIGINT        NOT NULL DEFAULT 0,
    creado_en          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ,
    creado_por         UUID,
    actualizado_por    UUID,
    CONSTRAINT uq_abonos_idempotencia UNIQUE (tenant_id, clave_idempotencia),
    CONSTRAINT ck_abonos_monto   CHECK (monto > 0),
    CONSTRAINT ck_abonos_metodo  CHECK (metodo IN ('EFECTIVO', 'TARJETA', 'NEQUI', 'DAVIPLATA', 'BRE_B', 'OTRO')),
    CONSTRAINT ck_abonos_motivo  CHECK (motivo_anulacion IS NULL OR motivo_anulacion IN ('DEVOLUCION', 'ERROR_REGISTRO')),
    CONSTRAINT ck_abonos_anulado CHECK (NOT anulado OR (motivo_anulacion IS NOT NULL AND razon_anulacion IS NOT NULL))
);
CREATE INDEX ix_abonos_pedido ON abonos(pedido_id);
CREATE INDEX ix_abonos_tenant_creado ON abonos(tenant_id, creado_en);

-- ═══════════════════════════════════════════════════════════════════
--  Datos de referencia (necesarios en todos los ambientes)
-- ═══════════════════════════════════════════════════════════════════

-- Roles de acceso (SUPER_ADMIN vive en la cuenta, no como rol de acceso).
INSERT INTO roles (id, nombre) VALUES
    ('00000000-0000-0000-0000-000000000010', 'SASTRE'),
    ('00000000-0000-0000-0000-000000000011', 'SASTRE_ADMIN');
