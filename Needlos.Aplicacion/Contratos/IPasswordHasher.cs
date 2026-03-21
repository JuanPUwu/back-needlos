namespace Needlos.Aplicacion.Contratos;

public interface IPasswordHasher
{
    string Hash(string password);
    bool Verify(string password, string hash);
}
