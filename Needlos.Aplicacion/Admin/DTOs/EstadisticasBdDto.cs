namespace Needlos.Aplicacion.Admin.DTOs;

public record EstadisticaTablaDto(
    string Tabla,
    long   FilasEstimadas,
    long   TamanoTotalBytes,
    string TamanoTotal,
    long   TamanoDatosBytes,
    string TamanoDatos,
    long   TamanoIndicesBytes,
    string TamanoIndices
);

public record EstadisticasBdDto(
    long                      TamanoBdBytes,
    string                    TamanoBd,
    List<EstadisticaTablaDto> Tablas
);
