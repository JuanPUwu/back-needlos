using FluentValidation;

namespace Needlos.Aplicacion.Auth.Comandos.Login;

public class LoginValidator : AbstractValidator<LoginCommand>
{
    public LoginValidator()
    {
        // Login solo verifica que el campo no esté vacío; el formato de email
        // ya fue validado en el momento del registro. Esto permite que el
        // SuperAdmin semilla (email: "admin") pueda autenticarse.
        RuleFor(x => x.Email)
            .NotEmpty().WithMessage("El email es obligatorio.");

        RuleFor(x => x.Password)
            .NotEmpty().WithMessage("La contraseña es obligatoria.");
    }
}
