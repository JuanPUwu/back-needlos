using FluentValidation;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Clientes.Consultas.ObtenerClientes;

public class ObtenerClientesValidator : AbstractValidator<ObtenerClientesQuery>
{
    public ObtenerClientesValidator() => this.ReglaPaginacion();
}
