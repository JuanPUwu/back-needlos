using FluentValidation;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Ordenes.Consultas.ObtenerOrdenes;

public class ObtenerOrdenesValidator : AbstractValidator<ObtenerOrdenesQuery>
{
    public ObtenerOrdenesValidator() => this.ReglaPaginacion();
}
