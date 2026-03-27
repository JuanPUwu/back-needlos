using FluentValidation;
using Needlos.Dominio.Enumeraciones;

namespace Needlos.Aplicacion.Ordenes.Comandos.CambiarEstadoPrenda;

public class CambiarEstadoPrendaValidator : AbstractValidator<CambiarEstadoPrendaCommand>
{
    public CambiarEstadoPrendaValidator()
    {
        RuleFor(x => x.OrdenId)
            .NotEmpty().WithMessage("El id de la orden es obligatorio.");

        RuleFor(x => x.PrendaId)
            .NotEmpty().WithMessage("El id de la prenda es obligatorio.");

        RuleFor(x => x.NuevoEstado)
            .IsInEnum().WithMessage("El estado indicado no es válido. Valores: 0=EnProceso, 1=Finalizado, 2=Entregado.");
    }
}
