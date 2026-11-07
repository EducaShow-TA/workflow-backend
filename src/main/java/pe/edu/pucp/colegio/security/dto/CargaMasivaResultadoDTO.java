package pe.edu.pucp.colegio.security.dto;

import java.util.List;

public record CargaMasivaResultadoDTO(
    boolean exitoso,
    int totalFilas,
    int usuariosCreados,
    int usuariosActualizados,
    int usuariosVinculados,
    int usuariosSinCambios,
    int filasRechazadas,
    List<ErrorFilaDTO> errores,
    List<CuentaCargadaDTO> cuentas,
    List<FilaCargaMasivaResultadoDTO> resultados) {}
