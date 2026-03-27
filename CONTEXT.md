# NeedlOS — Contexto del Proyecto para Claude

## ¿Qué es NeedlOS?

SaaS multi-tenant para **gestión de sastrerías**. Cada tienda (tenant) gestiona sus propias órdenes, clientes y pagos de forma completamente aislada mediante una columna `TenantId` en todas las tablas.

Stack: **.NET 10 · ASP.NET Core · Entity Framework Core 10 · PostgreSQL · MediatR 12 · FluentValidation 11 · JWT**

---

## Arquitectura: Clean Architecture + Ports & Adapters

### Regla de oro — Dirección de dependencias

```
Needlos.Api
    ↓
Needlos.Aplicacion  ←── define contratos (interfaces/puertos)
    ↓
Needlos.Dominio

Needlos.Infraestructura  ←── implementa los contratos de Aplicacion
    ↓
Needlos.Aplicacion + Needlos.Dominio
```

**Aplicacion NUNCA referencia Infraestructura.**
**Dominio NUNCA referencia nada externo.**

### Responsabilidad de cada capa

| Proyecto | Responsabilidad | Puede referenciar |
|---|---|---|
| `Needlos.Dominio` | Entidades, enumeraciones, reglas de negocio puras | Nada externo |
| `Needlos.Aplicacion` | Casos de uso (Commands/Queries/Handlers), DTOs, interfaces/contratos | Dominio + EF Core (solo para INeedlosDbContext) |
| `Needlos.Infraestructura` | Adaptadores: DbContext, JWT, hashing, tenancy | Dominio + Aplicacion |
| `Needlos.Api` | Controllers HTTP, configuración DI, Program.cs | Aplicacion + Infraestructura |

---

## Estructura de carpetas

```
NeedlOS/
├── CLAUDE.md
├── needlos.slnx
│
├── Needlos.Dominio/
│   ├── Entidades/
│   │   ├── EntidadBase.cs   ← base de entidades tenant (Id, TenantId, Eliminado, CreadoEn, ActualizadoEn, CreadoPor, ActualizadoPor)
│   │   ├── Orden.cs         ← hereda EntidadBase; TipoOrden, navegación a Prendas y Pagos
│   │   ├── Prenda.cs        ← hereda EntidadBase; CambiarEstado() lanza BusinessException si ya Entregada
│   │   ├── Cliente.cs       ← hereda EntidadBase; Nombre, Apellido, Telefono
│   │   ├── Pago.cs          ← hereda EntidadBase
│   │   ├── TipoPrenda.cs    ← entidad global (no hereda EntidadBase); catálogo de 10 tipos semilla
│   │   ├── Tenant.cs        ← entidad global
│   │   ├── Usuario.cs       ← entidad global
│   │   ├── Rol.cs           ← entidad global
│   │   └── UsuarioRol.cs    ← tabla intermedia (PK compuesta)
│   ├── Excepciones/
│   │   └── BusinessException.cs  ← excepción de dominio puro → 400
│   └── Enumeraciones/
│       ├── EstadoPrenda.cs  ← EnProceso=0, Finalizado=1, Entregado=2
│       ├── TipoOrden.cs     ← Arreglo=0, Confeccion=1
│       └── MetodoPago.cs    ← Efectivo=0, Transferencia=1, Tarjeta=2
│
├── Needlos.Aplicacion/
│   ├── Contratos/                  ← PUERTOS (interfaces que Infraestructura implementa)
│   │   ├── INeedlosDbContext.cs
│   │   ├── ITenantProvider.cs
│   │   ├── IJwtService.cs
│   │   └── IPasswordHasher.cs
│   ├── Excepciones/                ← excepciones de aplicación (se convierten a HTTP en el middleware)
│   │   ├── NotFoundException.cs    → 404
│   │   ├── ConflictException.cs   → 409
│   │   └── ValidationException.cs → 400 (lleva lista de errores de FluentValidation)
│   ├── Behaviors/
│   │   └── ValidationBehavior.cs  ← pipeline MediatR: valida antes de cada handler automáticamente
│   ├── Shared/                     ← lógica reutilizable entre features (no son repositorios)
│   │   ├── ClienteService.cs  ← ValidarExistenciaAsync(clienteId) → NotFoundException
│   │   ├── OrdenService.cs    ← ValidarExistenciaAsync(ordenId)   → NotFoundException
│   │   ├── PaginadoDto.cs     ← wrapper genérico de respuesta paginada
│   │   └── RolesConstantes.cs ← IDs y nombres de roles del sistema (Admin, SuperAdmin)
│   ├── TiposPrendas/
│   │   ├── Consultas/ObtenerTiposPrendas/
│   │   │   ├── ObtenerTiposPrendasQuery.cs
│   │   │   └── ObtenerTiposPrendasHandler.cs
│   │   └── DTOs/TipoPrendaDto.cs
│   ├── Admin/
│   │   ├── Comandos/ConfigurarSuperAdmin/
│   │   │   ├── ConfigurarSuperAdminCommand.cs
│   │   │   ├── ConfigurarSuperAdminHandler.cs
│   │   │   └── ConfigurarSuperAdminValidator.cs
│   │   ├── Consultas/ObtenerTenants/
│   │   │   ├── ObtenerTenantsQuery.cs
│   │   │   ├── ObtenerTenantsHandler.cs
│   │   │   └── ObtenerTenantsValidator.cs
│   │   ├── Consultas/ObtenerUsuariosPorTenant/
│   │   │   ├── ObtenerUsuariosPorTenantQuery.cs
│   │   │   ├── ObtenerUsuariosPorTenantHandler.cs
│   │   │   └── ObtenerUsuariosPorTenantValidator.cs
│   │   └── DTOs/
│   │       ├── TenantAdminDto.cs
│   │       └── UsuarioAdminDto.cs
│   ├── Ordenes/
│   │   ├── Comandos/
│   │   │   ├── CrearOrden/
│   │   │   │   ├── CrearOrdenCommand.cs      (contiene también PrendaRequest)
│   │   │   │   ├── CrearOrdenHandler.cs
│   │   │   │   └── CrearOrdenValidator.cs
│   │   │   └── CambiarEstadoPrenda/
│   │   │       ├── CambiarEstadoPrendaCommand.cs
│   │   │       ├── CambiarEstadoPrendaHandler.cs
│   │   │       └── CambiarEstadoPrendaValidator.cs
│   │   ├── Consultas/
│   │   │   ├── ObtenerOrdenes/
│   │   │   │   ├── ObtenerOrdenesQuery.cs
│   │   │   │   ├── ObtenerOrdenesHandler.cs  ← expone MapearOrden() internal static para reutilizar
│   │   │   │   └── ObtenerOrdenesValidator.cs
│   │   │   └── ObtenerOrdenPorId/
│   │   │       ├── ObtenerOrdenPorIdQuery.cs
│   │   │       └── ObtenerOrdenPorIdHandler.cs
│   │   └── DTOs/
│   │       └── OrdenDto.cs         (contiene también PrendaDto)
│   ├── Clientes/       (misma estructura: Comandos/ Consultas/ DTOs/ — cada Comando tiene su Validator)
│   ├── Pagos/          (Comandos/ + Consultas/ + DTOs/)
│   └── Auth/
│       ├── Comandos/Login/     (LoginCommand + LoginHandler + LoginValidator)
│       ├── Comandos/Registrar/ (RegistrarTenantCommand + RegistrarTenantHandler + RegistrarTenantValidator)
│       └── DTOs/               (LoginResultDto)
│
├── Needlos.Infraestructura/
│   ├── Auth/
│   │   ├── JwtService.cs           implements IJwtService
│   │   └── BcryptPasswordHasher.cs implements IPasswordHasher
│   ├── Tenancy/
│   │   └── TenantProvider.cs       implements ITenantProvider (lee "tenant_id" y "sub" del JWT)
│   ├── Datos/
│   │   ├── NeedlosDbContext.cs      implements INeedlosDbContext (: DbContext, INeedlosDbContext)
│   │   └── NeedlosDbContextFactory.cs  (para migraciones EF Core — usa DesignTimeTenantProvider dummy)
│   └── Migrations/
│
└── Needlos.Api/
    ├── Controllers/
    │   ├── AuthController.cs             (público: /api/auth/registrar, /api/auth/login)
    │   ├── AdminController.cs            ([Authorize(Roles="SuperAdmin")] en todos los endpoints)
    │   ├── OrdenesController.cs          ([Authorize(Roles="Admin,SuperAdmin")])
    │   ├── ClientesController.cs         ([Authorize(Roles="Admin,SuperAdmin")])
    │   ├── TipoPrendasController.cs      ([Authorize(Roles="Admin,SuperAdmin")], ruta: /api/tipos-prendas)
    │   └── PagosController.cs            ([Authorize(Roles="Admin,SuperAdmin")])
    ├── Middleware/
    │   ├── CorrelationIdMiddleware.cs     ← asigna X-Correlation-Id a cada request
    │   ├── RequestLoggingMiddleware.cs    ← loguea método, path, status, duración y tenant
    │   └── ExceptionHandlerMiddleware.cs  ← convierte excepciones → JSON con status code correcto
    ├── Program.cs
    └── appsettings.json
```

---

## Modelo de datos

### Enumeraciones

```
EstadoPrenda: EnProceso=0, Finalizado=1, Entregado=2
TipoOrden:    Arreglo=0, Confeccion=1
MetodoPago:   Efectivo=0, Transferencia=1, Tarjeta=2
```

Se almacenan como **string** en PostgreSQL (`HasConversion<string>()`).
Al enviarlos por JSON en requests se usan como **entero** (valor del enum).

### Entidades por tenant (heredan `EntidadBase`)

> `EntidadBase` aporta: `Id (Guid)`, `TenantId (Guid)`, `Eliminado (bool)`, `CreadoEn`, `ActualizadoEn`, `CreadoPor (Guid)`, `ActualizadoPor (Guid?)`

```
Cliente
├── Nombre, Apellido, Telefono, FechaRegistro
└── → Ordenes (1:N)

Orden
├── ClienteId (FK → Cliente, restrict)
├── TipoOrden (TipoOrden, default: Arreglo)
├── → Prendas (1:N Prenda)         ← cascade delete
└── → Pagos (1:N Pago)

Prenda
├── OrdenId (FK → Orden, cascade delete)
├── TipoPrendaId (FK → TipoPrenda, restrict)
├── Cantidad (int), Descripcion, PrecioPorUnidad (decimal)
├── FechaEntrega (DateOnly)
└── Estado (EstadoPrenda, default: EnProceso)

Pago
├── OrdenId (FK → Orden, restrict)
├── Monto, Metodo (MetodoPago), Fecha
```

**Campos derivados en `OrdenDto` (calculados al leer, NO almacenados en BD):**
- `Estado` = estado más joven (menor valor de `EstadoPrenda`) entre todas las prendas
- `FechaEntrega` = fecha más próxima entre todas las prendas
- `PrecioTotal` = suma de `PrecioPorUnidad × Cantidad` de cada prenda

### Entidades globales (NO heredan EntidadBase, sin filtro tenant)

```
TipoPrenda
└── Id, Nombre  ← catálogo fijo (10 registros semilla, IDs fijos 00000000-0000-0000-0001-000000000001..10)

Tenant
└── Id, Nombre, Slug (único, auto-generado), Activo, CreadoEn

Usuario
├── Id, Email (único), PasswordHash, TenantId (FK → Tenant), Telefono (NOT NULL), Activo
└── → Roles (N:M via UsuarioRol)

Rol
└── Id, Nombre

UsuarioRol
└── UsuarioId + RolId (PK compuesta)
```

### Roles del sistema (sembrados en BD — IDs fijos)

| Rol | ID fijo | Descripción |
|---|---|---|
| `SuperAdmin` | `00000000-0000-0000-0000-000000000001` | Creador del sistema. Acceso global a todos los tenants. Se crean sin límite via `POST /api/admin/superadmins` (requiere SuperAdmin). Uno está pre-sembrado en BD (email: `admin`, password: `admin`) como bootstrap inicial. |
| `Admin` | `00000000-0000-0000-0000-000000000002` | Dueño/encargado de una sastrería. Se asigna automáticamente al registrarse. |

El **Tenant sistema** (`Id = 00000000-0000-0000-0000-000000000003`, slug `"sistema"`) es el tenant al que pertenece el SuperAdmin. Está reservado y no puede ser tomado por ninguna sastrería.

El **SuperAdmin semilla** tiene `Id = 00000000-0000-0000-0000-000000000004`, email `admin`, contraseña `admin` y teléfono `3133585900`. Está insertado directamente en BD y su email no pasa por el validador de formato (es una cuenta de sistema). Cambiar la contraseña en producción.

Los nombres de los roles (`"Admin"`, `"SuperAdmin"`) se almacenan en BD tal cual (sin normalización, ya que son constantes del sistema). Deben coincidir exactamente con los valores en `RolesConstantes` y con los atributos `[Authorize(Roles = "...")]` de los controllers.

---

## Multi-tenancy

- Estrategia: **columna TenantId** en todas las tablas de negocio (no schemas separados)
- `NeedlosDbContext` captura `_tenantId` y `_usuarioId` en el constructor desde `ITenantProvider`
- `HasQueryFilter` aplica automáticamente `TenantId == _tenantId && !Eliminado` a todas las entidades tenant
- `SaveChangesAsync` override establece automáticamente `TenantId`, timestamps y `CreadoPor`/`ActualizadoPor` en entidades nuevas/modificadas
- `TenantProvider` lee el claim `"tenant_id"` y `"sub"` del JWT; retorna `Guid.Empty` para endpoints públicos (auth)
- Las entidades globales (Tenant, Usuario, Rol) no tienen filtro → accesibles sin tenant

---

## Soft Delete

No existe DELETE real en ningún endpoint. El borrado lógico se hace:

1. Cargar la entidad
2. Poner `entidad.Eliminado = true`
3. `SaveChangesAsync` — el query filter `!Eliminado` la excluirá de todas las consultas futuras

El query filter se aplica automáticamente a todas las entidades tenant, por lo que los registros con `Eliminado = true` son invisibles sin necesidad de filtros manuales.

---

## Normalización de datos en BD

Toda string que se guarda pasa automáticamente por `NeedlosDbContext.SaveChangesAsync`:

- **Trim** — elimina espacios al inicio y al final
- **Espacios internos** — colapsa múltiples espacios en uno (`"Juan  Pérez"` → `"juan pérez"`)
- **Lowercase** — todo en minúsculas

**Campo excluido:** `PasswordHash` (BCrypt es case-sensitive, nunca se normaliza).

Si se agregan campos sensibles futuros (tokens, claves API, etc.), añadirlos al `HashSet _camposExcluidos` en `NeedlosDbContext`.

Los enums (`EstadoOrden`, `MetodoPago`) no se ven afectados porque su `ClrType` no es `string`.

---

## Slug auto-generado

El `Slug` del tenant se genera automáticamente en `RegistrarTenantHandler` a partir del `NombreTienda`:

- Elimina acentos/diacríticos (`á→a`, `é→e`, `ñ→n`, `ü→u`)
- Convierte a lowercase
- Reemplaza espacios por guion `-`
- Elimina caracteres especiales (`&`, `@`, `.`, etc.)
- Si el slug ya existe en BD, agrega sufijo numérico: `-2`, `-3`...

```
"Sastrería Juan"        → sastreria-juan
"Moda & Elegancia"      → moda-elegancia
"Sastrería Juan" (2ª)   → sastreria-juan-2
"Taller Ñoño"           → taller-nono
```

---

## Manejo de errores

### Flujo

```
Request recibido
    ↓
[ValidationBehavior] → si hay errores → ValidationException
    ↓ si ok
Handler → puede lanzar NotFoundException / ConflictException / BusinessException
    ↓ (cualquier excepción)
ExceptionHandlerMiddleware (Needlos.Api/Middleware/)
    ↓
Convierte a JSON: { "mensaje": "...", "errores": [...] } + status code HTTP
```

### Mapa completo de excepciones → HTTP

| Excepción | Proyecto | HTTP | Cuándo usarla |
|---|---|---|---|
| `ValidationException` | Aplicacion/Excepciones | 400 | Input inválido (lanzada automáticamente por ValidationBehavior) |
| `BusinessException` | Dominio/Excepciones | 400 | Regla de negocio violada dentro de una entidad |
| `NotFoundException` | Aplicacion/Excepciones | 404 | Recurso no encontrado (orden, cliente, etc.) |
| `ConflictException` | Aplicacion/Excepciones | 409 | Recurso ya existe (email duplicado, slug duplicado) |
| `UnauthorizedAccessException` | BCL (.NET) | 401 | Credenciales inválidas o usuario inactivo |
| `Exception` (cualquier otra) | — | 500 | Error inesperado — el mensaje real NO se expone al cliente |

### 401 y 403 del middleware JWT

Los errores de autenticación/autorización que genera el middleware JWT de ASP.NET Core **no lanzan excepciones** — se interceptan con `JwtBearerEvents` en `Program.cs` y retornan JSON directamente:

| Escenario | HTTP | Mensaje |
|---|---|---|
| Sin token o token mal formado | 401 | `"Se requiere autenticación. Incluye un token JWT válido en el header Authorization."` |
| Token expirado | 401 | `"El token ha expirado."` |
| Token con firma inválida | 401 | `"El token tiene una firma inválida."` |
| Token válido pero rol insuficiente | 403 | `"No tienes permisos para acceder a este recurso."` |

Respuesta JSON: `{ "mensaje": "..." }` — mismo formato que el resto de errores.

### Reglas

- Los handlers **lanzan excepciones**; los controllers **nunca** tienen try-catch
- Los errores de negocio van en handlers, no en controllers
- Operaciones que retornan "no encontrado" usan `NotFoundException`, no `bool`
- Los errores 500 se loguean en servidor pero solo devuelven `"Ha ocurrido un error inesperado."` al cliente

### Ejemplo de handler correcto

```csharp
public async Task<Unit> Handle(ActualizarEstadoOrdenCommand request, CancellationToken ct)
{
    var orden = await _context.Ordenes.FirstOrDefaultAsync(o => o.Id == request.OrdenId, ct);
    if (orden is null)
        throw new NotFoundException($"Orden '{request.OrdenId}' no encontrada.");

    orden.Estado = request.NuevoEstado;
    await _context.SaveChangesAsync(ct);
    return Unit.Value;
}
```

---

## Validación de inputs (FluentValidation)

### Arquitectura

```
Request llega al controller
    ↓
Controller llama _mediator.Send(command)
    ↓
[ValidationBehavior] ← pipeline automático de MediatR
    ↓ si hay errores → ValidationException → middleware → 400
    ↓ si todo ok
Handler se ejecuta
```

El `ValidationBehavior` está registrado como `IPipelineBehavior<,>` y se ejecuta automáticamente para **todos los requests**. Si el command tiene un `IValidator<T>` registrado y hay errores, lanza `ValidationException` antes de llegar al handler. Si no hay validator, pasa directo.

### Validators existentes

Cada command tiene su validator co-ubicado en la misma carpeta:

```
Aplicacion/
├── Auth/Comandos/Login/                        LoginValidator.cs
├── Auth/Comandos/Registrar/                    RegistrarTenantValidator.cs
├── Admin/Comandos/ConfigurarSuperAdmin/        ConfigurarSuperAdminValidator.cs
├── Admin/Consultas/ObtenerTenants/             ObtenerTenantsValidator.cs           ← pagina/tamano
├── Admin/Consultas/ObtenerUsuariosPorTenant/   ObtenerUsuariosPorTenantValidator.cs ← pagina/tamano
├── Clientes/Comandos/CrearCliente/             CrearClienteValidator.cs
├── Clientes/Comandos/ActualizarCliente/        ActualizarClienteValidator.cs
├── Clientes/Consultas/ObtenerClientes/         ObtenerClientesValidator.cs      ← pagina/tamano
├── MedidasCliente/Comandos/CrearMedidasCliente/     CrearMedidasClienteValidator.cs
├── MedidasCliente/Comandos/ActualizarMedidasCliente/ ActualizarMedidasClienteValidator.cs
├── Ordenes/Comandos/CrearOrden/                CrearOrdenValidator.cs
├── Ordenes/Comandos/ActualizarEstadoOrden/     ActualizarEstadoOrdenValidator.cs
├── Ordenes/Consultas/ObtenerOrdenes/           ObtenerOrdenesValidator.cs       ← pagina/tamano
├── Pagos/Comandos/CrearPago/                   CrearPagoValidator.cs
├── Servicios/Comandos/CrearServicio/           CrearServicioValidator.cs
├── Servicios/Comandos/ActualizarServicio/      ActualizarServicioValidator.cs
└── Servicios/Consultas/ObtenerServicios/       ObtenerServiciosValidator.cs     ← pagina/tamano
```

### Reglas por command

| Command | Reglas |
|---|---|
| `RegistrarTenantCommand` | NombreTienda required ≤100, Email válido ≤150, Password ≥8 chars + mayúscula + minúscula + número + especial, Telefono required ≥10 dígitos ≤20 chars |
| `LoginCommand` | Email required (sin validación de formato — permite el SuperAdmin semilla con email `admin`), Password required |
| `ConfigurarSuperAdminCommand` | Email válido ≤150, Password ≥8 chars + mayúscula + minúscula + número + especial, Telefono required ≥10 dígitos ≤20 chars |
| `CrearClienteCommand` | Nombre required ≤100, Apellido required ≤100, Telefono required ≥7 ≤20 chars |
| `ActualizarClienteCommand` | Id not empty, Nombre required ≤100, Apellido required ≤100, Telefono required ≥7 ≤20 chars |
| `CrearOrdenCommand` | ClienteId not empty, al menos 1 prenda; por prenda: TipoPrendaId not empty, Cantidad ≥1, Descripcion required ≤500, PrecioPorUnidad > 0, FechaEntrega ≥ hoy |
| `CambiarEstadoPrendaCommand` | OrdenId not empty, PrendaId not empty, NuevoEstado valor de enum válido (0-2) |
| `CrearPagoCommand` | OrdenId not empty, Monto > 0, Metodo valor de enum válido |

### Formato de respuesta en error de validación (400)

```json
{
  "mensaje": "Errores de validación",
  "errores": [
    "El nombre es obligatorio.",
    "El email no tiene un formato válido."
  ]
}
```

### Cómo agregar un validator a un nuevo command

```csharp
// Aplicacion/[Feature]/Comandos/[Accion]/[Accion]Validator.cs
public class CrearDescuentoValidator : AbstractValidator<CrearDescuentoCommand>
{
    public CrearDescuentoValidator()
    {
        RuleFor(x => x.Nombre).NotEmpty().WithMessage("El nombre es obligatorio.");
        RuleFor(x => x.Porcentaje).InclusiveBetween(1, 100).WithMessage("El porcentaje debe estar entre 1 y 100.");
    }
}
```

No hay nada más que hacer: `AddValidatorsFromAssembly` lo registra automáticamente.

---

## Modelo de dominio rico

Las entidades de negocio no son solo contenedores de datos. Cuando existen **reglas de negocio reales**, se expresan como métodos en la propia entidad. Esto garantiza que la invariante se cumple sin importar desde dónde se modifique la entidad.

### Regla actual en `Prenda`

```csharp
// Dominio/Entidades/Prenda.cs
public void CambiarEstado(EstadoPrenda nuevoEstado)
{
    if (Estado == EstadoPrenda.Entregado)
        throw new BusinessException("La prenda ya fue entregada y no puede cambiar de estado.");
    Estado = nuevoEstado;
}
```

El handler llama `prenda.CambiarEstado(estado)` en vez de `prenda.Estado = estado` directamente. Si la prenda ya está Entregada, lanza `BusinessException` → middleware → 400.

El **estado de la orden** no se almacena — se calcula al leer como el valor mínimo de `EstadoPrenda` entre todas sus prendas (el estado más joven en el flujo).

### Cuándo agregar lógica a una entidad

Solo cuando haya una **invariante real del negocio** (no solo CRUD). Ejemplos futuros:
- `Pago.Aplicar()` → verificar que no exceda el total de la orden

---

## CQRS con MediatR

### Patrón para cada feature

```
Aplicacion/[Feature]/
├── Comandos/[NombreAccion]/
│   ├── [NombreAccion]Command.cs    → record : IRequest<T>
│   └── [NombreAccion]Handler.cs   → class : IRequestHandler<Command, T>
├── Consultas/[NombreConsulta]/
│   ├── [NombreConsulta]Query.cs   → record : IRequest<T>
│   └── [NombreConsulta]Handler.cs → class : IRequestHandler<Query, T>
└── DTOs/
    └── [Feature]Dto.cs
```

### Registración MediatR

```csharp
// Solo un assembly — Aplicacion tiene Commands, Queries Y Handlers
builder.Services.AddMediatR(cfg =>
    cfg.RegisterServicesFromAssembly(typeof(LoginCommand).Assembly));
```

### Handlers inyectan contratos, no implementaciones

```csharp
// CORRECTO
public class CrearOrdenHandler(INeedlosDbContext context) : IRequestHandler<...>

// MAL — viola Clean Architecture
public class CrearOrdenHandler(NeedlosDbContext context) : IRequestHandler<...>
```

---

## Autenticación

- **Registro**: `POST /api/auth/registrar` — crea Tenant + Usuario admin (con rol `Admin`), slug auto-generado
- **Login**: `POST /api/auth/login` — valida credenciales, retorna JWT con el rol real leído de BD
- JWT contiene claims: `sub` (userId), `email`, `tenant_id`, `role`
- Todos los endpoints de negocio llevan `[Authorize(Roles = "Admin,SuperAdmin")]`
- `AuthController` es el único controller completamente público (sin `[Authorize]`)
- Los 401/403 retornan JSON `{ "mensaje": "..." }` vía `JwtBearerEvents` configurados en `Program.cs` — **no pasan por `ExceptionHandlerMiddleware`** porque el middleware JWT actúa antes

### Comportamiento de 401 y 403

| Escenario | HTTP | Mensaje devuelto |
|---|---|---|
| Sin token / token mal formado | 401 | `"Se requiere autenticación. Incluye un token JWT válido en el header Authorization."` |
| Token expirado | 401 | `"El token ha expirado."` |
| Token con firma inválida | 401 | `"El token tiene una firma inválida."` |
| Token válido pero rol insuficiente | 403 | `"No tienes permisos para acceder a este recurso."` |

---

## Endpoints — contratos de request/response

> Todos los errores devuelven `{ "mensaje": "descripción del error" }` con el status code correspondiente.

### Auth (público)

```
POST /api/auth/registrar
Body:       { "nombreTienda": string, "email": string, "password": string, "telefono": string }
201 Created { "tenantId": guid }
409 Conflict — el email ya está registrado

POST /api/auth/login
Body:    { "email": string, "password": string }
200 OK   { "token": string, "tenantId": guid, "email": string }
401 Unauthorized — credenciales inválidas o usuario inactivo
```

### Admin sistema `[Authorize(Roles="SuperAdmin")]`

```
POST /api/admin/superadmins
Body:        { "email": string, "password": string, "telefono": string }
201 Created  { "id": guid }
400 Bad Request — input inválido
401 Unauthorized — no autenticado
403 Forbidden — no tiene rol SuperAdmin
409 Conflict — el email ya está registrado

GET /api/admin/tenants?pagina=1&tamano=20
200 OK  PaginadoDto<TenantAdminDto>  { datos, pagina, tamano, total, totalPaginas }
400 Bad Request — parámetros de paginación inválidos

GET /api/admin/tenants/{tenantId}/usuarios?pagina=1&tamano=20
200 OK  PaginadoDto<UsuarioAdminDto>
400 Bad Request — parámetros inválidos
404 Not Found — tenant no encontrado
```

### Tipos de prenda `[Authorize(Roles="Admin,SuperAdmin")]`

```
GET /api/tipos-prendas
200 OK  TipoPrendaDto[]  (lista completa ordenada alfabéticamente, sin paginar)
```

### Clientes `[Authorize(Roles="Admin,SuperAdmin")]`

```
GET /api/clientes?pagina=1&tamano=20&telefono=
200 OK  PaginadoDto<ClienteDto>  { datos, pagina, tamano, total, totalPaginas }
        Ordenado por apellido + nombre. Filtro opcional por teléfono (búsqueda parcial).
400 Bad Request — parámetros de paginación inválidos

GET /api/clientes/{id}
200 OK  ClienteDto
404 Not Found — cliente no encontrado

POST /api/clientes
Body:        { "nombre": string, "apellido": string, "telefono": string }
201 Created  { "id": guid }
400 Bad Request — input inválido

PUT /api/clientes/{id}
Body:        { "nombre": string, "apellido": string, "telefono": string }
204 No Content
400 Bad Request — input inválido
404 Not Found — cliente no encontrado

DELETE /api/clientes/{id}
204 No Content
404 Not Found — cliente no encontrado
```

### Órdenes `[Authorize(Roles="Admin,SuperAdmin")]`

```
GET /api/ordenes?pagina=1&tamano=20
200 OK  PaginadoDto<OrdenDto>
400 Bad Request — parámetros de paginación inválidos

GET /api/ordenes/{id}
200 OK  OrdenDto  (incluye prendas con estado individual)
404 Not Found — orden no encontrada

POST /api/ordenes
Body: {
  "clienteId": guid,
  "tipoOrden": int,           ← 0=Arreglo (default), 1=Confeccion
  "prendas": [{
    "tipoPrendaId": guid,
    "cantidad": int,
    "descripcion": string,
    "precioPorUnidad": decimal,
    "fechaEntrega": "YYYY-MM-DD"   ← DateOnly, no puede ser anterior al día actual
  }]
}
201 Created { "id": guid }
400 Bad Request — input inválido (sin prendas, fecha pasada, precio negativo, etc.)
404 Not Found — clienteId o tipoPrendaId no existe

PUT /api/ordenes/{id}/prendas/{prendaId}/estado
Body: int  ← 0=EnProceso, 1=Finalizado, 2=Entregado
204 No Content
400 Bad Request — estado inválido o prenda ya entregada
404 Not Found — orden o prenda no encontrada
```

### Pagos `[Authorize(Roles="Admin,SuperAdmin")]`

```
GET /api/pagos?ordenId={guid}
200 OK  PagoDto[]  (lista ordenada por fecha ascendente)
404 Not Found — la ordenId no existe

POST /api/pagos
Body:        { "ordenId": guid, "monto": decimal, "metodo": int }
             metodo: 0=Efectivo, 1=Transferencia, 2=Tarjeta (ASP.NET Core deserializa automáticamente a MetodoPago)
201 Created  { "id": guid }
400 Bad Request — input inválido (validación)
404 Not Found — la ordenId no existe
```


---

## DTOs de respuesta

```
OrdenDto
├── id, clienteId, nombreCliente, apellidoCliente, tipoOrden (string), creadoEn
├── estado (string)      ← derivado: estado más joven entre prendas
├── precioTotal (decimal) ← derivado: suma de (precioPorUnidad × cantidad) de prendas
├── fechaEntrega (DateOnly) ← derivada: fecha más próxima entre prendas
└── prendas: PrendaDto[]
       └── id, tipoPrendaId, tipoPrenda (string), cantidad, descripcion,
           precioPorUnidad, precioTotal, fechaEntrega (DateOnly), estado (string)

ClienteDto
└── id, nombre, apellido, telefono, fechaRegistro

TipoPrendaDto
└── id, nombre

PagoDto
└── id, ordenId, monto, metodo (string), fecha

TenantAdminDto
└── id, nombre, slug, activo, creadoEn

UsuarioAdminDto
└── id, email, activo, roles (string[])
```

---

## Cómo agregar una nueva feature

**Ejemplo: agregar `Descuento`**

1. **Dominio** — crear `Needlos.Dominio/Entidades/Descuento.cs` heredando `EntidadBase`
2. **Aplicacion** — crear carpeta `Needlos.Aplicacion/Descuentos/` con:
   - `Comandos/CrearDescuento/CrearDescuentoCommand.cs` + `CrearDescuentoHandler.cs` + `CrearDescuentoValidator.cs`
   - `Consultas/ObtenerDescuentos/ObtenerDescuentosQuery.cs` + `ObtenerDescuentosHandler.cs`
   - `DTOs/DescuentoDto.cs`
3. **Infraestructura** — en `INeedlosDbContext` agregar `DbSet<Descuento> Descuentos { get; }`, en `NeedlosDbContext` agregar el DbSet + query filter en `OnModelCreating` + relaciones si aplica
4. **Api** — crear `Needlos.Api/Controllers/DescuentosController.cs` con `[Authorize]`; los handlers lanzan `NotFoundException`/`ConflictException` según corresponda, los controllers no tienen try-catch
5. **Migración** — `dotnet ef migrations add AgregarDescuentos --project Needlos.Infraestructura --startup-project Needlos.Api`
6. **Documentación** — actualizar `CLAUDE.md`: agregar los endpoints nuevos con sus status codes, errores y cuerpos de request/response en la sección "Endpoints"

---

## Reglas que NUNCA se deben romper

1. **Handlers siempre en `Needlos.Aplicacion`**, junto a su Command/Query
2. **Interfaces/contratos siempre en `Needlos.Aplicacion/Contratos/`**
3. **`Needlos.Aplicacion` nunca referencia `Needlos.Infraestructura`**
4. **Handlers inyectan `INeedlosDbContext`, no `NeedlosDbContext`**
5. **Controllers solo llaman `_mediator.Send(...)` — ninguna lógica de negocio en controllers**
6. **Todas las entidades de negocio heredan `EntidadBase`** (TenantId, soft delete y timestamps automáticos)
7. **Tenant, Usuario, Rol y TipoPrenda son globales** — no heredan EntidadBase, sin filtro de tenant
8. **Nuevos controllers deben llevar `[Authorize(Roles = "Admin,SuperAdmin")]`** para endpoints de negocio, o `[Authorize(Roles = "SuperAdmin")]` para endpoints de gestión global. Solo `AuthController` es completamente público.
   **`TipoPrenda` es global pero requiere auth** — es un catálogo del sistema, no público.
9. **Enums se almacenan como string en PostgreSQL** (legibilidad en BD)
10. **No hay DELETE real** — siempre soft delete (`Eliminado = true`)
11. **Toda nueva feature se documenta en CLAUDE.md** — endpoints con request/response, status codes y errores posibles. Sin documentación, la feature está incompleta.
12. **Handlers lanzan excepciones, controllers no tienen try-catch** — el middleware global convierte las excepciones a JSON automáticamente
13. **Todo nuevo command lleva su validator** — `[Accion]Validator.cs` co-ubicado con el command. `AddValidatorsFromAssembly` lo registra automáticamente.
14. **Lógica de negocio en entidades, no en handlers** — si existe una invariante real (transición inválida, límite de negocio), se expresa en un método de la entidad que lanza `BusinessException`
15. **Lógica reutilizable entre handlers va en `Aplicacion/Shared/`** — si la misma verificación aparece en 2+ handlers, se extrae a un servicio shared. No antes (no es pre-optimización).
16. **Todos los endpoints de lista son paginados** — retornan `PaginadoDto<T>` con parámetros `?pagina` y `?tamano`. Nunca devolver listas sin paginar.
17. **Todo endpoint lleva documentación Swagger descriptiva y amigable** — usar `/// <summary>`, `/// <remarks>` y `/// <response code="...">` en cada acción del controller. Las descripciones deben explicar qué hace el endpoint, qué espera y qué devuelve en lenguaje claro (no técnico). Ver controllers existentes como referencia de estilo.

---

## Servicios de aplicación compartidos (Shared/)

Lógica reutilizable que varios handlers podrían necesitar. No son repositorios ni abstracciones de EF Core — son servicios dentro de la misma capa de Aplicacion que evitan duplicar queries entre handlers.

| Servicio | Método | Comportamiento |
|---|---|---|
| `ClienteService` | `ValidarExistenciaAsync(clienteId)` | Lanza `NotFoundException` si el cliente no existe en el tenant |
| `OrdenService` | `ValidarExistenciaAsync(ordenId)` | Lanza `NotFoundException` si la orden no existe en el tenant |

**Cuándo agregar un nuevo servicio shared:** cuando la misma query de verificación aparece (o aparecería) en 2 o más handlers. No antes.

---

## Paginación

Todos los endpoints de lista usan paginación estandarizada. Los parámetros van como query string.

### Request
```
GET /api/ordenes?pagina=2&tamano=10
```
Valores default: `pagina=1`, `tamano=20`. Límite máximo: `tamano=100`.

### Response
```json
{
  "datos": [...],
  "pagina": 2,
  "tamano": 10,
  "total": 347,
  "totalPaginas": 35
}
```

`totalPaginas` se calcula automáticamente en `PaginadoDto<T>`. El frontend no necesita hacer una query extra para saber cuántas páginas hay.

### Orden de resultados
- Órdenes: `OrderByDescending(CreadoEn)` — las más recientes primero
- Clientes: `OrderBy(Apellido).ThenBy(Nombre)` — alfabético por apellido

> **Excepción:** `GET /api/tipos-prendas` no es paginado — el catálogo es fijo (10 registros) y se devuelve completo.
- Servicios: `OrderBy(Nombre)` — alfabético

### Cómo agregar paginación a una nueva feature
1. El Query record acepta `(int Pagina = 1, int Tamano = 20)`
2. El Handler retorna `PaginadoDto<T>` con `CountAsync` + `Skip/Take`
3. Agregar `[Feature]Validator` para el Query con las reglas de rango
4. El Controller recibe `[FromQuery] int pagina = 1, [FromQuery] int tamano = 20`

---

## Observabilidad

### CorrelationId
Cada request tiene un ID de correlación para trazar logs relacionados.
- Si el cliente envía `X-Correlation-Id` en el header, se respeta.
- Si no, se genera un GUID nuevo.
- Siempre se devuelve en el header de respuesta `X-Correlation-Id`.

### Logs estructurados
`RequestLoggingMiddleware` registra cada request al finalizar:
```
HTTP POST /api/ordenes → 201 en 45ms | correlationId=abc-123 | tenant=guid
```

Niveles:
- 2xx/3xx → `Information`
- 4xx → `Warning`
- 5xx → `Error` (además, `ExceptionHandlerMiddleware` loguea el stack trace completo)

### Índices filtrados (soft delete + unicidad)
Los índices únicos en PostgreSQL usan `HasFilter` para ignorar registros inactivos:
- `Tenant.Slug` — único solo entre tenants con `Activo = true`
- `Usuario.Email` — único solo entre usuarios con `Activo = true`

Esto permite reutilizar un slug o email si el registro original fue desactivado, sin que la constraint de BD lo bloquee.

---

## Migraciones

### Flujo de desarrollo — `dev.sh`

En desarrollo se usa `dev.sh` para reiniciar todo desde cero (equivalente a `ddl-auto=create` de Spring Boot):

```bash
./dev.sh
```

El script:
1. Elimina la BD (`database drop --force`)
2. Elimina todos los archivos `.cs` de `Migrations/` (migración + designer + snapshot)
3. Regenera `InitialCreate` desde el modelo actual (`migrations add InitialCreate`)
4. Aplica la migración (`database update`) — crea tablas + datos semilla
5. Arranca la API (`dotnet run`)

Cualquier cambio en entidades, relaciones o configuraciones de EF queda reflejado automáticamente la próxima vez que se corra `dev.sh`. No hace falta crear migraciones adicionales durante el desarrollo.

### Datos semilla

Los datos iniciales del sistema viven en `NeedlosDbContext.OnModelCreating` usando `HasData`. Esto garantiza que siempre se incluyen al regenerar `InitialCreate`:

- Roles `SuperAdmin` (`...0001`) y `Admin` (`...0002`)
- Tenant sistema (`...0003`, slug `"sistema"`)
- SuperAdmin semilla (`...0004`, email `admin`, password `admin`)
- `UsuarioRol` que asigna SuperAdmin al usuario semilla

**No se deben crear migraciones separadas para datos semilla** — todo va en `HasData`.

### Flujo de producción

En producción las migraciones deben estar commiteadas y no se regeneran:

```bash
# Crear nueva migración (cuando se hace un cambio de modelo)
dotnet ef migrations add [NombreMigracion] \
  --project Needlos.Infraestructura \
  --startup-project Needlos.Api

# Aplicar a la BD (sin perder datos)
dotnet ef database update \
  --project Needlos.Infraestructura \
  --startup-project Needlos.Api
```

Conexión BD: `Host=localhost;Port=5432;Database=needlos_db;Username=postgres;Password=admin`

---

## Configuración JWT

La clave JWT **nunca** se almacena en `appsettings.json` (está vacía en el repositorio).

- **Desarrollo:** `appsettings.Development.json` (no commitear con secretos reales en producción)
- **Producción:** variable de entorno `Jwt__Key` (doble guion bajo = separador de sección en ASP.NET Core)

El arranque falla inmediatamente si la clave no está configurada.

```json
"Jwt": {
  "Key": "",
  "Issuer": "NeedlOS",
  "Audience": "NeedlOS",
  "ExpiracionHoras": "24"
}
```

---

## Advertencias conocidas (no son errores)

- `NU1603`: `System.IdentityModel.Tokens.Jwt` resuelve 8.4.0 en lugar de 8.3.3 — inofensivo
