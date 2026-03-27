using System.Security.Cryptography;
using System.Text;

namespace Needlos.Aplicacion.Shared;

/// <summary>
/// Utilidades para generación y hashing de refresh tokens.
/// SHA-256 es suficiente aquí porque el token es aleatorio de 64 bytes,
/// no una contraseña predecible (para contraseñas se usa BCrypt).
/// </summary>
internal static class TokenHasher
{
    internal static string GenerarRaw() =>
        Convert.ToBase64String(RandomNumberGenerator.GetBytes(64));

    internal static string Hash(string token) =>
        Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(token))).ToLowerInvariant();
}
