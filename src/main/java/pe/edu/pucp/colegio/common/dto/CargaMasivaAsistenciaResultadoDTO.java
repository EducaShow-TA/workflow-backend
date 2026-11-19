package pe.edu.pucp.colegio.common.dto;

import java.util.List;
import pe.edu.pucp.colegio.security.dto.ErrorFilaDTO;

/**
 * Resumen de una carga masiva de asistencia (US-2.2 y US-2.5). La carga es de todo o nada: si hay
 * errores o solo se pidió validar, {@code guardado} es falso y no se modificó ningún registro.
 */
public record CargaMasivaAsistenciaResultadoDTO(
    boolean guardado,
    boolean soloValidacion,
    int totalFilas,
    int filasValidas,
    int filasRechazadas,
    int registrosNuevos,
    int registrosActualizados,
    List<ErrorFilaDTO> errores) {}
