using FluentValidation;

namespace Needlos.Aplicacion.Shared;

public static class ValidacionExtensions
{
    /// <summary>
    /// Aplica las reglas de contraseña segura: mínimo 8 caracteres,
    /// mayúscula, minúscula, número y carácter especial.
    /// </summary>
    public static IRuleBuilderOptions<T, string> ReglaContrasena<T>(
        this IRuleBuilder<T, string> rule) =>
        rule
            .NotEmpty().WithMessage("La contraseña es obligatoria.")
            .MinimumLength(8).WithMessage("La contraseña debe tener al menos 8 caracteres.")
            .Matches("[A-Z]").WithMessage("La contraseña debe tener al menos una letra mayúscula.")
            .Matches("[a-z]").WithMessage("La contraseña debe tener al menos una letra minúscula.")
            .Matches("[0-9]").WithMessage("La contraseña debe tener al menos un número.")
            .Matches("[^a-zA-Z0-9]").WithMessage("La contraseña debe tener al menos un carácter especial.");

    /// <summary>
    /// Aplica las reglas de paginación estándar: pagina >= 1, tamano entre 1 y 100.
    /// El tipo T debe implementar IPaginadoQuery.
    /// </summary>
    public static void ReglaPaginacion<T>(this AbstractValidator<T> validator)
        where T : class, IPaginadoQuery
    {
        validator.RuleFor(x => x.Pagina)
            .GreaterThanOrEqualTo(1).WithMessage("La página debe ser mayor o igual a 1.");

        validator.RuleFor(x => x.Tamano)
            .InclusiveBetween(1, 100).WithMessage("El tamaño de página debe estar entre 1 y 100.");
    }
}
