using FluentValidation;

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

        RuleFor(x => x.Password)
            .NotEmpty().WithMessage("La contraseña es obligatoria.")
            .MinimumLength(6).WithMessage("La contraseña debe tener al menos 6 caracteres.");
    }
}
