using MediatR;
using Needlos.Aplicacion.Servicios.DTOs;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Servicios.Consultas.ObtenerServicios;

public record ObtenerServiciosQuery(int Pagina = 1, int Tamano = 20)
    : IRequest<PaginadoDto<ServicioDto>>;
