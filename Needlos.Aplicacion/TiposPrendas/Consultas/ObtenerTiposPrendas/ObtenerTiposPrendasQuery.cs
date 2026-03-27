using MediatR;
using Needlos.Aplicacion.TiposPrendas.DTOs;

namespace Needlos.Aplicacion.TiposPrendas.Consultas.ObtenerTiposPrendas;

public record ObtenerTiposPrendasQuery : IRequest<List<TipoPrendaDto>>;
