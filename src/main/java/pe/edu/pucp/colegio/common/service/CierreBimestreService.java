package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.dto.CierreBimestreResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.model.BimestreCalendario;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.BimestreCalendarioRepository;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@Service
public class CierreBimestreService {

  private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final CalendarioBimestralService calendario;
  private final BimestreCalendarioRepository bimestres;
  private final ConsolidacionLogroService consolidacion;
  private final CuentaUsuarioRepository cuentas;
  private final RegistroAuditoriaRepository auditorias;
  private final Clock clock;

  public CierreBimestreService(
      CalendarioBimestralService calendario,
      BimestreCalendarioRepository bimestres,
      ConsolidacionLogroService consolidacion,
      CuentaUsuarioRepository cuentas,
      RegistroAuditoriaRepository auditorias,
      Clock clock) {
    this.calendario = calendario;
    this.bimestres = bimestres;
    this.consolidacion = consolidacion;
    this.cuentas = cuentas;
    this.auditorias = auditorias;
    this.clock = clock;
  }

  @Transactional
  public CierreBimestreResponse cerrar(int anhio, int numero, String username) {
    if (numero < 1 || numero > CalendarioBimestralService.BIMESTRES) {
      throw new BusinessException("El bimestre debe estar entre 1 y 4");
    }
    List<BimestreCalendario> calendarioAnhio = calendario.asegurar(anhio);
    BimestreCalendario bimestre = calendarioAnhio.get(numero - 1);
    if (bimestre.isCerrado()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El bimestre " + numero + " ya está cerrado");
    }
    LocalDate hoy = LocalDate.now(clock);
    if (!hoy.isAfter(bimestre.getFechaFin())) {
      throw new BusinessException(
          "El bimestre "
              + numero
              + " termina el "
              + bimestre.getFechaFin().format(FORMATO)
              + "; podrás cerrarlo desde el "
              + bimestre.getFechaFin().plusDays(1).format(FORMATO));
    }
    if (numero > 1 && !calendarioAnhio.get(numero - 2).isCerrado()) {
      throw new BusinessException("Primero cierra el bimestre " + (numero - 1));
    }

    int completadas = 0;
    for (Periodo periodo : calendario.periodosDelAnhio(anhio)) {
      if (!Boolean.TRUE.equals(periodo.getActasCerradas())) {
        completadas += consolidacion.completarFaltantesDelBimestre(periodo, numero, username);
      }
    }
    bimestre.cerrar(LocalDateTime.now(clock), username);
    bimestres.save(bimestre);

    var cuenta = cuentas.findByUsername(username).orElse(null);
    auditorias.save(
        new RegistroAuditoria(
            cuenta == null ? null : cuenta.getId(),
            cuenta == null ? null : cuenta.getRol().name(),
            AccionAuditoria.CERRAR_BIMESTRE,
            "Cierre del bimestre "
                + numero
                + " del año "
                + anhio
                + " por "
                + username
                + ": "
                + completadas
                + " notas faltantes registradas como C"));
    return new CierreBimestreResponse(calendario.respuesta(bimestre), completadas);
  }
}
