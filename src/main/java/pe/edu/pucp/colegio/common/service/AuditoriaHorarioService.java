package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.AuditoriaHorarioResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.model.AuditoriaHorario;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;
import pe.edu.pucp.colegio.common.model.SesionHorario;
import pe.edu.pucp.colegio.common.repository.AuditoriaHorarioRepository;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/**
 * Registra y consulta el historial de cambios de horario (US-4.5). Corre dentro de la transacción
 * de quien lo llama: si el cambio se revierte, el registro también.
 */
@Service
public class AuditoriaHorarioService {
  private static final Locale ES = Locale.forLanguageTag("es-PE");
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
  private static final int MESES_POR_DEFECTO = 3;
  private final AuditoriaHorarioRepository auditoria;
  private final CuentaUsuarioRepository cuentas;
  private final Clock clock;

  public AuditoriaHorarioService(
      AuditoriaHorarioRepository auditoria, CuentaUsuarioRepository cuentas, Clock clock) {
    this.auditoria = auditoria;
    this.cuentas = cuentas;
    this.clock = clock;
  }

  @Transactional
  public void registrar(
      String username,
      HorarioSeccion horario,
      String accion,
      SesionHorario anterior,
      SesionHorario nuevo,
      String vigenciaTipo,
      LocalDate desde,
      LocalDate hasta) {
    if ("ACTUALIZACIÓN".equals(accion)
        && "PERMANENTE".equals(vigenciaTipo)
        && anterior != null
        && nuevo != null
        && detalle(anterior).equals(detalle(nuevo))) {
      // Guardar la sesión sin cambiar bloque, curso, docente ni aula no es un cambio de horario.
      return;
    }
    var cuenta = cuentas.findByUsername(username).orElse(null);
    AuditoriaHorario item = new AuditoriaHorario();
    item.setHorarioId(horario.getId());
    item.setSeccion(
        horario.getSeccion().getGrado().getNumero() + "° " + horario.getSeccion().getCodigo());
    item.setAccion(accion);
    item.setFechaHora(LocalDateTime.now(clock));
    item.setUsuarioCuentaId(cuenta == null ? null : cuenta.getId());
    item.setUsuarioNombre(
        cuenta == null
            ? username
            : cuenta.getPersona() == null
                ? cuenta.getUsername()
                : cuenta.getPersona().getNombreCompleto());
    item.setDetalle(detalle(nuevo != null ? nuevo : anterior));
    item.setValoresAnteriores(anterior == null ? null : detalle(anterior));
    item.setValoresNuevos(nuevo == null ? null : detalle(nuevo));
    item.setVigenciaTipo(vigenciaTipo);
    item.setVigenteDesde(desde);
    item.setVigenteHasta(hasta);
    auditoria.save(item);
  }

  /**
   * Historial entre {@code desde} y {@code hasta}, ambos días completos. Sin {@code hasta} se usa
   * hoy (hora del servidor) y sin {@code desde}, los tres meses anteriores.
   */
  @Transactional(readOnly = true)
  public List<AuditoriaHorarioResponse> listar(
      Integer horarioId, LocalDate desdeDia, LocalDate hastaDia) {
    LocalDate fin = hastaDia == null ? LocalDate.now(clock) : hastaDia;
    LocalDate inicio = desdeDia == null ? fin.minusMonths(MESES_POR_DEFECTO) : desdeDia;
    if (inicio.isAfter(fin)) {
      throw new BusinessException("La fecha inicial no puede ser posterior a la fecha final");
    }
    LocalDateTime desde = inicio.atStartOfDay();
    LocalDateTime hasta = fin.atTime(LocalTime.MAX);
    List<AuditoriaHorario> registros =
        horarioId == null
            ? auditoria.findAllByFechaHoraBetweenOrderByFechaHoraDesc(desde, hasta)
            : auditoria.findAllByHorarioIdAndFechaHoraBetweenOrderByFechaHoraDesc(
                horarioId, desde, hasta);
    return registros.stream()
        .map(
            a ->
                new AuditoriaHorarioResponse(
                    a.getId(),
                    a.getHorarioId(),
                    a.getSeccion(),
                    a.getAccion(),
                    a.getFechaHora(),
                    a.getUsuarioCuentaId(),
                    a.getUsuarioNombre(),
                    a.getDetalle(),
                    a.getValoresAnteriores(),
                    a.getValoresNuevos(),
                    a.getVigenciaTipo(),
                    a.getVigenteDesde(),
                    a.getVigenteHasta()))
        .toList();
  }

  private String detalle(SesionHorario s) {
    return "Día "
        + s.getDia().getDisplayName(TextStyle.FULL, ES)
        + ", bloque "
        + HORA.format(s.getBloque().getHoraInicio())
        + "–"
        + HORA.format(s.getBloque().getHoraFin())
        + ", área "
        + s.getArea().getNombre()
        + ", docente "
        + s.getDocente().getPersona().getNombreCompleto()
        + ", aula "
        + s.getAula().getCodigo();
  }
}
