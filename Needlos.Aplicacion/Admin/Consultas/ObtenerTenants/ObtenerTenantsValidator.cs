using FluentValidation;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Admin.Consultas.ObtenerTenants;

public class ObtenerTenantsValidator : AbstractValidator<ObtenerTenantsQuery>
{
    public ObtenerTenantsValidator() => this.ReglaPaginacion();
}
