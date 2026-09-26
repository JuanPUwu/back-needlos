package com.needlos.clientes;

import com.needlos.clientes.domain.Cliente;
import com.needlos.clientes.dto.ClienteDtos.ActualizarClienteRequest;
import com.needlos.clientes.dto.ClienteDtos.ClienteResponse;
import com.needlos.clientes.dto.ClienteDtos.CrearClienteRequest;
import com.needlos.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository repo;

    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }

    public ClienteResponse crear(CrearClienteRequest req) {
        Cliente c = new Cliente();
        c.setNombre(req.nombre());
        c.setApellido(req.apellido());
        c.setTelefono(req.telefono());
        return toResponse(repo.save(c));
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(String buscar, Pageable pageable) {
        Page<Cliente> pagina = (buscar == null || buscar.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(buscar, buscar, pageable);
        return pagina.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(UUID id) {
        return toResponse(buscar(id));
    }

    public ClienteResponse actualizar(UUID id, ActualizarClienteRequest req) {
        Cliente c = buscar(id);
        c.setNombre(req.nombre());
        c.setApellido(req.apellido());
        c.setTelefono(req.telefono());
        return toResponse(repo.save(c));
    }

    public void eliminar(UUID id) {
        // Soft-delete: @SoftDelete convierte este delete en un UPDATE eliminado=true.
        repo.delete(buscar(id));
    }

    private Cliente buscar(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado."));
    }

    private ClienteResponse toResponse(Cliente c) {
        return new ClienteResponse(
                c.getId(), c.getNombre(), c.getApellido(), c.getTelefono(), c.getFechaRegistro());
    }
}
