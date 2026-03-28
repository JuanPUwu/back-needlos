using System.Text;
using System.Reflection;
using System.Threading.RateLimiting;
using DotNetEnv;
using FluentValidation;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;
using Needlos.Aplicacion.Auth.Comandos.Login;
using Needlos.Aplicacion.Behaviors;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Shared;
using Needlos.Api.Middleware;
using Needlos.Api.Swagger;
using Needlos.Infraestructura.Auth;
using Needlos.Infraestructura.Datos;
using Needlos.Infraestructura.Estadisticas;
using Needlos.Infraestructura.Tenancy;
using MediatR;

// ── Cargar .env ───────────────────────────────────────────────────
// NoClobber: las variables de entorno del sistema tienen prioridad sobre .env
// (en producción no existe .env — se usan las vars del sistema directamente).
// TraversePath: busca .env subiendo desde el directorio actual.
Env.NoClobber().TraversePath().Load();

var builder = WebApplication.CreateBuilder(args);

// ── Validaciones de configuración crítica ─────────────────────────
var jwtKey = builder.Configuration["Jwt:Key"];
if (string.IsNullOrWhiteSpace(jwtKey))
    throw new InvalidOperationException(
        "La clave JWT no está configurada. " +
        "Establece la variable de entorno 'Jwt__Key' o configura 'Jwt:Key' en appsettings.Development.json.");

var frontendUrl = builder.Configuration["Auth:FrontendUrl"];
if (string.IsNullOrWhiteSpace(frontendUrl))
    throw new InvalidOperationException(
        "La URL del frontend no está configurada. " +
        "Establece 'Auth:FrontendUrl' en appsettings.");


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
builder.Services.AddScoped<IEstadisticasBdService, EstadisticasBdService>();

// ── Servicios de aplicación compartidos ──────────────────────────
builder.Services.AddScoped<ClienteService>();
builder.Services.AddScoped<OrdenService>();

// ── MediatR ───────────────────────────────────────────────────────
builder.Services.AddMediatR(cfg =>
    cfg.RegisterServicesFromAssembly(typeof(LoginCommand).Assembly));

// ── FluentValidation ──────────────────────────────────────────────
builder.Services.AddValidatorsFromAssembly(typeof(LoginCommand).Assembly);

// ── ValidationBehavior (pipeline MediatR) ────────────────────────
builder.Services.AddTransient(typeof(IPipelineBehavior<,>), typeof(ValidationBehavior<,>));

// ── CORS ─────────────────────────────────────────────────────────
// AllowCredentials es obligatorio para que el navegador envíe la cookie HttpOnly del refresh token.
// WithOrigins exacto es más seguro que AllowAnyOrigin.
builder.Services.AddCors(options =>
{
    options.AddPolicy("needlos", policy =>
    {
        policy.WithOrigins(frontendUrl)
              .AllowCredentials()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

// ── Rate Limiting ─────────────────────────────────────────────────
// Limita /auth/login y /auth/refresh a 5 requests por minuto POR IP.
// Cada IP tiene su propio contador independiente (partitioned by RemoteIpAddress).
builder.Services.AddRateLimiter(options =>
{
    options.AddPolicy("auth", httpContext =>
        RateLimitPartition.GetFixedWindowLimiter(
            partitionKey: httpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown",
            factory: _ => new FixedWindowRateLimiterOptions
            {
                PermitLimit          = 5,
                Window               = TimeSpan.FromMinutes(1),
                QueueProcessingOrder = QueueProcessingOrder.OldestFirst,
                QueueLimit           = 0
            }));

    options.OnRejected = async (context, cancellationToken) =>
    {
        context.HttpContext.Response.StatusCode = StatusCodes.Status429TooManyRequests;
        await context.HttpContext.Response.WriteAsJsonAsync(
            new { mensaje = "Demasiados intentos. Espera un momento antes de volver a intentarlo." },
            cancellationToken);
    };
});

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
                      "Usa POST /api/auth/login para obtener el access token y luego haz clic en 'Authorize'."
    });

    var xmlFile = $"{Assembly.GetExecutingAssembly().GetName().Name}.xml";
    var xmlPath = Path.Combine(AppContext.BaseDirectory, xmlFile);
    c.IncludeXmlComments(xmlPath);

    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name         = "Authorization",
        Type         = SecuritySchemeType.Http,
        Scheme       = "Bearer",
        BearerFormat = "JWT",
        In           = ParameterLocation.Header,
        Description  = "Ingresa el access token obtenido en /api/auth/login. Ejemplo: eyJhbGci..."
    });

    c.OperationFilter<BearerAuthOperationFilter>();
});

// ── JWT Authentication ────────────────────────────────────────────
builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer           = true,
            ValidateAudience         = true,
            ValidateLifetime         = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer              = builder.Configuration["Jwt:Issuer"],
            ValidAudience            = builder.Configuration["Jwt:Audience"],
            IssuerSigningKey         = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey))
        };

        options.Events = new JwtBearerEvents
        {
            OnChallenge = async context =>
            {
                context.HandleResponse();

                var mensaje = context.AuthenticateFailure?.GetType().Name switch
                {
                    "SecurityTokenExpiredException"        => "El token ha expirado.",
                    "SecurityTokenInvalidSignatureException" => "El token tiene una firma inválida.",
                    _ when context.AuthenticateFailure != null => "El token es inválido.",
                    _ => "Se requiere autenticación. Incluye un token JWT válido en el header Authorization."
                };

                context.Response.StatusCode  = StatusCodes.Status401Unauthorized;
                context.Response.ContentType = "application/json";
                await context.Response.WriteAsJsonAsync(new { mensaje });
            },

            OnForbidden = async context =>
            {
                context.Response.StatusCode  = StatusCodes.Status403Forbidden;
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
app.UseMiddleware<CorrelationIdMiddleware>();
app.UseMiddleware<ExceptionHandlerMiddleware>();
app.UseMiddleware<RequestLoggingMiddleware>();

app.UseHttpsRedirection();
app.UseCors("needlos");
app.UseRateLimiter();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.Run();
