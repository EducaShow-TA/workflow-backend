package pe.edu.pucp.colegio.seguridad.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.seguridad.dto.CrearCuentaRequestDTO;
import pe.edu.pucp.colegio.seguridad.dto.CuentaResponseDTO;
import pe.edu.pucp.colegio.seguridad.model.CuentaUsuario;
import pe.edu.pucp.colegio.seguridad.repository.CuentaUsuarioRepository;
import pe.edu.pucp.colegio.seguridad.repository.PersonaRepository;
import pe.edu.pucp.colegio.seguridad.service.CuentaUsuarioService;

@Service
public class CuentaUsuarioServiceImpl implements CuentaUsuarioService {

  private static final Logger log = LoggerFactory.getLogger(CuentaUsuarioServiceImpl.class);

  private final CuentaUsuarioRepository cuentas;
  private final PersonaRepository personas;
  private final RegistroAuditoriaRepository auditorias;
  private final PasswordEncoder encoder;

  public CuentaUsuarioServiceImpl(
      CuentaUsuarioRepository cuentas,
      PersonaRepository personas,
      RegistroAuditoriaRepository auditorias,
      PasswordEncoder encoder) {
    this.cuentas = cuentas;
    this.personas = personas;
    this.auditorias = auditorias;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public CuentaResponseDTO crear(CrearCuentaRequestDTO request, String usernameResponsable) {
    if (cuentas.existsByUsername(request.username())) {
      throw new ConflictException("Ya existe una cuenta con ese correo");
    }
    if (personas.existsByDni(request.dni())) {
      throw new ConflictException("Ya existe una persona con ese DNI");
    }
    Persona persona =
        personas.save(
            new Persona(
                request.dni(),
                request.nombre(),
                request.apellidoPaterno(),
                request.apellidoMaterno()));
    CuentaUsuario cuenta =
        cuentas.save(
            new CuentaUsuario(
                request.username(), encoder.encode(request.password()), request.rol(), persona));
    var responsable = cuentas.findByUsername(usernameResponsable).orElse(null);
    auditorias.save(
        new RegistroAuditoria(
            responsable == null ? null : responsable.getId(),
            responsable == null ? null : responsable.getRol().name(),
            AccionAuditoria.CREAR,
            "Alta de cuenta " + cuenta.getUsername() + " con rol " + cuenta.getRol()));
    log.info("event=cuenta_creada id={} rol={} por={}", cuenta.getId(), cuenta.getRol(), usernameResponsable);
    return aDto(cuenta);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CuentaResponseDTO> listar() {
    return cuentas.findAll().stream().map(this::aDto).toList();
  }

  private CuentaResponseDTO aDto(CuentaUsuario c) {
    var p = c.getPersona();
    return new CuentaResponseDTO(
        c.getId(),
        c.getUsername(),
        c.getRol().name(),
        p == null ? c.getUsername() : p.getNombreCompleto(),
        p == null ? "" : p.getDni());
  }
}
