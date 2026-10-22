package pe.edu.pucp.colegio.security.dto;

import java.util.List;

public record CargaMasivaResultadoDTO(
    boolean exitoso, int totalFilas, List<ErrorFilaDTO> errores, List<CuentaCargadaDTO> cuentas) {}
