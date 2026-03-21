using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Contratos;

public interface IJwtService
{
    string GenerarToken(Usuario usuario, string rol);
}
