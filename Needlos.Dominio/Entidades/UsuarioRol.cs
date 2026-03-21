namespace Needlos.Dominio.Entidades;

public class UsuarioRol
{
    public Guid UsuarioId { get; set; }
    public Usuario? Usuario { get; set; }

    public Guid RolId { get; set; }
    public Rol? Rol { get; set; }
}
