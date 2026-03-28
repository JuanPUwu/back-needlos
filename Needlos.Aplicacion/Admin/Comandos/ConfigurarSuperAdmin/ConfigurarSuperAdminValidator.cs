using FluentValidation;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Admin.Comandos.ConfigurarSuperAdmin;

public class ConfigurarSuperAdminValidator : AbstractValidator<ConfigurarSuperAdminCommand>
{
    public ConfigurarSuperAdminValidator()
    {
        RuleFor(x => x.Email)
            .NotEmpty().WithMessage("El email es obligatorio.")
            .EmailAddress().WithMessage("El email no tiene un formato válido.")
            .MaximumLength(150).WithMessage("El email no puede superar los 150 caracteres.");

        RuleFor(x => x.Password).ReglaContrasena();

        RuleFor(x => x.Telefono)
            .NotEmpty().WithMessage("El teléfono es obligatorio.")
            .Must(t => t.Count(char.IsDigit) >= 10)
                .WithMessage("El teléfono debe contener al menos 10 dígitos.")
            .MaximumLength(20).WithMessage("El teléfono no puede superar los 20 caracteres.");
    }
}
