using FluentValidation;

namespace Needlos.Aplicacion.Pagos.Comandos.CrearPago;

public class CrearPagoValidator : AbstractValidator<CrearPagoCommand>
{
    public CrearPagoValidator()
    {
        RuleFor(x => x.OrdenId)
            .NotEmpty().WithMessage("El id de la orden es obligatorio.");

        RuleFor(x => x.Monto)
            .GreaterThan(0).WithMessage("El monto del pago debe ser mayor a 0.");

        RuleFor(x => x.Metodo)
            .IsInEnum().WithMessage("El método de pago no es válido. Valores permitidos: 0=Efectivo, 1=Transferencia, 2=Tarjeta.");
    }
}
