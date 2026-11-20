package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import pe.edu.pucp.colegio.common.audit.AccionAuditoriaFinanciera;
import pe.edu.pucp.colegio.common.audit.AuditoriaFinanciera;

public record AuditoriaFinancieraResponse(
    Integer id,
    LocalDateTime fechaHora,
    String usuario,
    String rolUsuario,
    AccionAuditoriaFinanciera accion,
    Integer compromisoId,
    Integer boletaId,
    Integer estudianteId,
    String estudianteNombre,
    String estudianteDni,
    Integer gradoNumero,
    String seccionCodigo,
    String apoderadoNombre,
    String concepto,
    LocalDate mesCobro,
    String estadoAnterior,
    String estadoNuevo,
    BigDecimal monto,
    Integer numeroCuotas,
    String mecanismo,
    String medioPago,
    String numeroComprobante) {

  public static AuditoriaFinancieraResponse desde(AuditoriaFinanciera a) {
    return new AuditoriaFinancieraResponse(
        a.getId(),
        a.getFechaHora(),
        a.getUsuario(),
        a.getRolUsuario(),
        a.getAccion(),
        a.getCompromisoId(),
        a.getBoletaId(),
        a.getEstudianteId(),
        a.getEstudianteNombre(),
        a.getEstudianteDni(),
        a.getGradoNumero(),
        a.getSeccionCodigo(),
        a.getApoderadoNombre(),
        a.getConcepto(),
        a.getMesCobro(),
        a.getEstadoAnterior(),
        a.getEstadoNuevo(),
        a.getMonto(),
        a.getNumeroCuotas(),
        a.getMecanismo(),
        a.getMedioPago(),
        a.getNumeroComprobante());
  }
}
