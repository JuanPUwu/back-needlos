using FluentValidation;

namespace Needlos.Aplicacion.Clientes.Comandos.CrearCliente;

public class CrearClienteValidator : AbstractValidator<CrearClienteCommand>
{
    public CrearClienteValidator()
    {
        RuleFor(x => x.Nombre)
            .NotEmpty().WithMessage("El nombre es obligatorio.")
            .MaximumLength(100).WithMessage("El nombre no puede superar 100 caracteres.");

        RuleFor(x => x.Apellido)
            .NotEmpty().WithMessage("El apellido es obligatorio.")
            .MaximumLength(100).WithMessage("El apellido no puede superar 100 caracteres.");

        RuleFor(x => x.Telefono)
            .NotEmpty().WithMessage("El teléfono es obligatorio.")
            .MinimumLength(7).WithMessage("El teléfono debe tener al menos 7 dígitos.")
            .MaximumLength(20).WithMessage("El teléfono no puede superar 20 caracteres.");
    }
}
