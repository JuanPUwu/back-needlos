using System.Text;
using System.Reflection;
using FluentValidation;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;
using Needlos.Aplicacion.Auth.Comandos.Login;
using Needlos.Aplicacion.Behaviors;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Shared;
using Needlos.Api.Middleware;
using Needlos.Infraestructura.Auth;
using Needlos.Infraestructura.Datos;
using Needlos.Infraestructura.Tenancy;
using MediatR;

var builder = WebApplication.CreateBuilder(args);

// ── DbContext ─────────────────────────────────────────────────────
builder.Services.AddDbContext<NeedlosDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection")));

// Registrar DbContext también como INeedlosDbContext
builder.Services.AddScoped<INeedlosDbContext>(sp =>
    sp.GetRequiredService<NeedlosDbContext>());

// ── HttpContextAccessor (requerido por TenantProvider) ────────────
builder.Services.AddHttpContextAccessor();

// ── Adaptadores (implementaciones de los contratos) ───────────────
builder.Services.AddScoped<ITenantProvider, TenantProvider>();
builder.Services.AddScoped<IJwtService, JwtService>();
builder.Services.AddScoped<IPasswordHasher, BcryptPasswordHasher>();

// ── Servicios de aplicación compartidos ──────────────────────────
// Lógica reutilizable entre handlers. No son repositorios:
// encapsulan verificaciones que varios handlers necesitarían duplicar.
builder.Services.AddScoped<ClienteService>();
builder.Services.AddScoped<OrdenService>();

// ── MediatR ───────────────────────────────────────────────────────
// Un solo assembly: Aplicacion contiene Commands, Queries y Handlers.
builder.Services.AddMediatR(cfg =>
    cfg.RegisterServicesFromAssembly(typeof(LoginCommand).Assembly));

// ── FluentValidation ──────────────────────────────────────────────
// Registra automáticamente todos los IValidator<T> del assembly Aplicacion.
builder.Services.AddValidatorsFromAssembly(typeof(LoginCommand).Assembly);

// ── ValidationBehavior (pipeline MediatR) ────────────────────────
// Se ejecuta antes de cada handler. Si el request tiene un validator
// registrado y hay errores, lanza ValidationException → 400.
builder.Services.AddTransient(typeof(IPipelineBehavior<,>), typeof(ValidationBehavior<,>));

// ── Controllers ───────────────────────────────────────────────────
builder.Services.AddControllers();

// ── Swagger ───────────────────────────────────────────────────────
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo
    {
        Title       = "NeedlOS API",
        Version     = "v1",
        Description = "API para la gestión de sastrerías. Todos los endpoints de negocio requieren autenticación JWT. " +
                      "Usa POST /api/auth/login para obtener el token y luego haz clic en 'Authorize'."
    });

    // Leer comentarios XML de los controllers
    var xmlFile = $"{Assembly.GetExecutingAssembly().GetName().Name}.xml";
    var xmlPath = Path.Combine(AppContext.BaseDirectory, xmlFile);
    c.IncludeXmlComments(xmlPath);

    // Botón Authorize en Swagger UI para enviar el JWT
    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name         = "Authorization",
        Type         = SecuritySchemeType.Http,
        Scheme       = "Bearer",
        BearerFormat = "JWT",
        In           = ParameterLocation.Header,
        Description  = "Ingresa el token JWT obtenido en /api/auth/login. Ejemplo: eyJhbGci..."
    });

    c.AddSecurityRequirement(_ => new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecuritySchemeReference("Bearer"),
            new List<string>()
        }
    });
});

// ── JWT Authentication ────────────────────────────────────────────
// En producción la clave debe venir de la variable de entorno Jwt__Key.
// En desarrollo se toma de appsettings.Development.json.
var jwtKey = builder.Configuration["Jwt:Key"];
if (string.IsNullOrWhiteSpace(jwtKey))
    throw new InvalidOperationException(
        "La clave JWT no está configurada. " +
        "Establece la variable de entorno 'Jwt__Key' o configura 'Jwt:Key' en appsettings.Development.json.");

builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidAudience = builder.Configuration["Jwt:Audience"],
            IssuerSigningKey = new SymmetricSecurityKey(
                Encoding.UTF8.GetBytes(jwtKey))
        };

        options.Events = new JwtBearerEvents
        {
            // Sin token o token inválido/expirado → 401
            OnChallenge = async context =>
            {
                context.HandleResponse();

                var mensaje = context.AuthenticateFailure?.GetType().Name switch
                {
                    "SecurityTokenExpiredException" => "El token ha expirado.",
                    "SecurityTokenInvalidSignatureException" => "El token tiene una firma inválida.",
                    _ when context.AuthenticateFailure != null => "El token es inválido.",
                    _ => "Se requiere autenticación. Incluye un token JWT válido en el header Authorization."
                };

                context.Response.StatusCode = StatusCodes.Status401Unauthorized;
                context.Response.ContentType = "application/json";
                await context.Response.WriteAsJsonAsync(new { mensaje });
            },

            // Token válido pero rol insuficiente → 403
            OnForbidden = async context =>
            {
                context.Response.StatusCode = StatusCodes.Status403Forbidden;
                context.Response.ContentType = "application/json";
                await context.Response.WriteAsJsonAsync(new
                {
                    mensaje = "No tienes permisos para acceder a este recurso."
                });
            }
        };
    });

// ── Build ─────────────────────────────────────────────────────────
var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// ── Pipeline de middleware (el orden importa) ─────────────────────
// 1. CorrelationId: primero de todo, para que los demás middleware puedan usarlo
app.UseMiddleware<CorrelationIdMiddleware>();

// 2. ExceptionHandler: antes que RequestLogging para que el status code
//    ya esté seteado cuando el logger registre la respuesta
app.UseMiddleware<ExceptionHandlerMiddleware>();

// 3. RequestLogging: al final del pipeline de middleware personalizado,
//    ya conoce el status code real de la respuesta
app.UseMiddleware<RequestLoggingMiddleware>();

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.Run();
