using FluentValidation;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Auth.Comandos.Registrar;

public class RegistrarTenantValidator : AbstractValidator<RegistrarTenantCommand>
{
    public RegistrarTenantValidator()
    {
        RuleFor(x => x.NombreTienda)
            .NotEmpty().WithMessage("El nombre de la tienda es obligatorio.")
            .MaximumLength(100).WithMessage("El nombre de la tienda no puede superar 100 caracteres.");

        RuleFor(x => x.Email)
            .NotEmpty().WithMessage("El email es obligatorio.")
            .EmailAddress().WithMessage("El email no tiene un formato válido.")
            .MaximumLength(150).WithMessage("El email no puede superar 150 caracteres.");

        RuleFor(x => x.Password).ReglaContrasena();

        RuleFor(x => x.Telefono)
            .NotEmpty().WithMessage("El teléfono es obligatorio.")
            .Must(t => t.Count(char.IsDigit) >= 10)
                .WithMessage("El teléfono debe contener al menos 10 dígitos.")
            .MaximumLength(20).WithMessage("El teléfono no puede superar los 20 caracteres.");
    }
}
