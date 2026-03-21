using FluentValidation;

namespace Needlos.Aplicacion.MedidasCliente.Comandos.CrearMedidasCliente;

public class CrearMedidasClienteValidator : AbstractValidator<CrearMedidasClienteCommand>
{
    public CrearMedidasClienteValidator()
    {
        RuleFor(x => x.ClienteId)
            .NotEmpty().WithMessage("El id del cliente es obligatorio.");

        RuleFor(x => x.Pecho)
            .GreaterThan(0).WithMessage("El pecho debe ser mayor que cero.");

        RuleFor(x => x.Cintura)
            .GreaterThan(0).WithMessage("La cintura debe ser mayor que cero.");

        RuleFor(x => x.Largo)
            .GreaterThan(0).WithMessage("El largo debe ser mayor que cero.");
    }
}
