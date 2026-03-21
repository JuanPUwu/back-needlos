using MediatR;
using Needlos.Aplicacion.Clientes.DTOs;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Clientes.Consultas.ObtenerClientes;

public record ObtenerClientesQuery(int Pagina = 1, int Tamano = 20)
    : IRequest<PaginadoDto<ClienteDto>>;
