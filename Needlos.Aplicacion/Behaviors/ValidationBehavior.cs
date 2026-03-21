using FluentValidation;
using MediatR;
using ValidationException = Needlos.Aplicacion.Excepciones.ValidationException;

namespace Needlos.Aplicacion.Behaviors;

/// <summary>
/// Pipeline behavior de MediatR que se ejecuta automáticamente antes de CADA handler.
///
/// Flujo:
///   Controller → Mediator → [ValidationBehavior] → Handler
///
/// Si el request tiene un IValidator<T> registrado y hay errores, lanza ValidationException
/// antes de que el handler siquiera se ejecute. Si no hay validator, pasa directo.
///
/// Esto elimina la necesidad de validar manualmente dentro de los handlers.
/// </summary>
public class ValidationBehavior<TRequest, TResponse>(IEnumerable<IValidator<TRequest>> validators)
    : IPipelineBehavior<TRequest, TResponse>
    where TRequest : notnull
{
    public async Task<TResponse> Handle(
        TRequest request,
        RequestHandlerDelegate<TResponse> next,
        CancellationToken cancellationToken)
    {
        if (!validators.Any())
            return await next();

        var context = new ValidationContext<TRequest>(request);

        var errores = validators
            .Select(v => v.Validate(context))
            .SelectMany(r => r.Errors)
            .Where(f => f is not null)
            .Select(f => f.ErrorMessage)
            .Distinct()
            .ToList();

        if (errores.Count > 0)
            throw new ValidationException(errores);

        return await next();
    }
}
