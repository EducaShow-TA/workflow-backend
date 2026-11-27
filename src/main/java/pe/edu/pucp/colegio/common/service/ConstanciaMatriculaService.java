package pe.edu.pucp.colegio.common.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;

@Service
public class ConstanciaMatriculaService {

  private final MatriculaRepository matriculas;
  private final ConstanciaMatriculaPdfService pdf;

  public ConstanciaMatriculaService(
      MatriculaRepository matriculas, ConstanciaMatriculaPdfService pdf) {
    this.matriculas = matriculas;
    this.pdf = pdf;
  }

  @Transactional(readOnly = true)
  public byte[] generar(Integer matriculaId) {
    var matricula =
        matriculas
            .findByIdAndActiveTrue(matriculaId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la matrícula"));
    if (matricula.getEstado() != EstadoMatricula.CONFIRMADA
        && matricula.getEstado() != EstadoMatricula.FINALIZADA) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La constancia estará disponible al confirmar la matrícula");
    }
    return pdf.generar(matricula);
  }
}
