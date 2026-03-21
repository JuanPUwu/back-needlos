using FluentValidation;

namespace Needlos.Aplicacion.MedidasCliente.Comandos.ActualizarMedidasCliente;

public class ActualizarMedidasClienteValidator : AbstractValidator<ActualizarMedidasClienteCommand>
{
    public ActualizarMedidasClienteValidator()
    {
        RuleFor(x => x.Id)
            .NotEmpty().WithMessage("El id de las medidas es obligatorio.");

        RuleFor(x => x.Pecho)
            .GreaterThan(0).WithMessage("El pecho debe ser mayor que cero.");

        RuleFor(x => x.Cintura)
            .GreaterThan(0).WithMessage("La cintura debe ser mayor que cero.");

        RuleFor(x => x.Largo)
            .GreaterThan(0).WithMessage("El largo debe ser mayor que cero.");
    }
}
