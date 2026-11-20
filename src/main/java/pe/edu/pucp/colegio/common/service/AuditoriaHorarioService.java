package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.AuditoriaHorarioResponse;
import pe.edu.pucp.colegio.common.model.AuditoriaHorario;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;
import pe.edu.pucp.colegio.common.model.SesionHorario;
import pe.edu.pucp.colegio.common.repository.AuditoriaHorarioRepository;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@Service
public class AuditoriaHorarioService {
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
      java.time.LocalDate desde,
      java.time.LocalDate hasta) {
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

  @Transactional(readOnly = true)
  public List<AuditoriaHorarioResponse> listar(
      Integer horarioId, LocalDateTime desde, LocalDateTime hasta) {
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
        + s.getDia()
        + ", bloque "
        + s.getBloque().getHoraInicio()
        + "–"
        + s.getBloque().getHoraFin()
        + ", área "
        + s.getArea().getNombre()
        + ", docente "
        + s.getDocente().getPersona().getNombreCompleto()
        + ", aula "
        + s.getAula().getCodigo();
  }
}
