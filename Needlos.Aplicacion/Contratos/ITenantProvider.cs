namespace Needlos.Aplicacion.Contratos;

public interface ITenantProvider
{
    Guid GetTenantId();
    Guid GetUsuarioId();
}
