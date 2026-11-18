package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import pe.edu.pucp.colegio.common.audit.AccionAuditoriaFinanciera;
import pe.edu.pucp.colegio.common.model.MedioPago;

/** Filtros opcionales de la auditoría financiera; las fechas incluyen el día completo. */
public record FiltroAuditoriaFinanciera(
    LocalDate desde,
    LocalDate hasta,
    AccionAuditoriaFinanciera accion,
    MedioPago medioPago,
    String estudiante,
    Integer gradoId,
    Integer seccionId,
    String comprobante,
    Integer boletaId) {}
