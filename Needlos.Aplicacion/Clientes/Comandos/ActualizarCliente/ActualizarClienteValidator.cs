using FluentValidation;

namespace Needlos.Aplicacion.Clientes.Comandos.ActualizarCliente;

public class ActualizarClienteValidator : AbstractValidator<ActualizarClienteCommand>
{
    public ActualizarClienteValidator()
    {
        RuleFor(x => x.Id)
            .NotEmpty().WithMessage("El id del cliente es obligatorio.");

        RuleFor(x => x.Nombre)
            .NotEmpty().WithMessage("El nombre es obligatorio.")
            .MaximumLength(100).WithMessage("El nombre no puede superar los 100 caracteres.");

        RuleFor(x => x.Telefono)
            .NotEmpty().WithMessage("El teléfono es obligatorio.")
            .MaximumLength(20).WithMessage("El teléfono no puede superar los 20 caracteres.");

        RuleFor(x => x.Email)
            .EmailAddress().WithMessage("El email no tiene un formato válido.")
            .MaximumLength(150).WithMessage("El email no puede superar los 150 caracteres.")
            .When(x => !string.IsNullOrWhiteSpace(x.Email));
    }
}
