using FluentValidation;

namespace Needlos.Aplicacion.Ordenes.Comandos.ActualizarEstadoOrden;

public class ActualizarEstadoOrdenValidator : AbstractValidator<ActualizarEstadoOrdenCommand>
{
    public ActualizarEstadoOrdenValidator()
    {
        RuleFor(x => x.OrdenId)
            .NotEmpty().WithMessage("El id de la orden es obligatorio.");

        RuleFor(x => x.NuevoEstado)
            .IsInEnum().WithMessage("El estado proporcionado no es válido.");
    }
}
