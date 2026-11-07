package pe.edu.pucp.colegio.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_calificaciones")
public class AuditoriaCalificacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "fecha_hora", nullable = false)
  private LocalDateTime fechaHora;

  @Column(nullable = false, length = 100)
  private String usuario;

  @Column(name = "rol_usuario", nullable = false, length = 30)
  private String rolUsuario;

  @Column(nullable = false, length = 40)
  private String accion;

  @Column(name = "tipo_calificacion", nullable = false, length = 20)
  private String tipoCalificacion;

  @Column(name = "periodo_id", nullable = false)
  private Integer periodoId;

  @Column(name = "matricula_id", nullable = false)
  private Integer matriculaId;

  @Column(name = "estudiante_id", nullable = false)
  private Integer estudianteId;

  @Column(name = "competencia_codigo", nullable = false, length = 50)
  private String competenciaCodigo;

  @Column(name = "evaluacion_id")
  private Integer evaluacionId;

  @Column(name = "nota_anterior", length = 2)
  private String notaAnterior;

  @Column(name = "nota_nueva", length = 2)
  private String notaNueva;

  @Column(name = "puntaje_anterior", precision = 6, scale = 4)
  private BigDecimal puntajeAnterior;

  @Column(name = "puntaje_nuevo", precision = 6, scale = 4)
  private BigDecimal puntajeNuevo;

  @Column(name = "consolidada_anterior")
  private Boolean consolidadaAnterior;

  @Column(name = "consolidada_nueva")
  private Boolean consolidadaNueva;

  @Column(name = "justificada_anterior")
  private Boolean justificadaAnterior;

  @Column(name = "justificada_nueva")
  private Boolean justificadaNueva;

  @Column(name = "observacion_anterior", length = 1000)
  private String observacionAnterior;

  @Column(name = "observacion_nueva", length = 1000)
  private String observacionNueva;

  protected AuditoriaCalificacion() {}

  public AuditoriaCalificacion(
      LocalDateTime fechaHora,
      String usuario,
      String rolUsuario,
      String accion,
      String tipoCalificacion,
      Integer periodoId,
      Integer matriculaId,
      Integer estudianteId,
      String competenciaCodigo,
      Integer evaluacionId,
      String notaAnterior,
      String notaNueva,
      BigDecimal puntajeAnterior,
      BigDecimal puntajeNuevo,
      Boolean consolidadaAnterior,
      Boolean consolidadaNueva,
      Boolean justificadaAnterior,
      Boolean justificadaNueva,
      String observacionAnterior,
      String observacionNueva) {
    this.fechaHora = fechaHora;
    this.usuario = usuario;
    this.rolUsuario = rolUsuario;
    this.accion = accion;
    this.tipoCalificacion = tipoCalificacion;
    this.periodoId = periodoId;
    this.matriculaId = matriculaId;
    this.estudianteId = estudianteId;
    this.competenciaCodigo = competenciaCodigo;
    this.evaluacionId = evaluacionId;
    this.notaAnterior = notaAnterior;
    this.notaNueva = notaNueva;
    this.puntajeAnterior = puntajeAnterior;
    this.puntajeNuevo = puntajeNuevo;
    this.consolidadaAnterior = consolidadaAnterior;
    this.consolidadaNueva = consolidadaNueva;
    this.justificadaAnterior = justificadaAnterior;
    this.justificadaNueva = justificadaNueva;
    this.observacionAnterior = observacionAnterior;
    this.observacionNueva = observacionNueva;
  }

  public Integer getId() {
    return id;
  }

  public LocalDateTime getFechaHora() {
    return fechaHora;
  }

  public String getUsuario() {
    return usuario;
  }

  public String getRolUsuario() {
    return rolUsuario;
  }

  public String getAccion() {
    return accion;
  }

  public String getTipoCalificacion() {
    return tipoCalificacion;
  }

  public Integer getPeriodoId() {
    return periodoId;
  }

  public Integer getMatriculaId() {
    return matriculaId;
  }

  public Integer getEstudianteId() {
    return estudianteId;
  }

  public String getCompetenciaCodigo() {
    return competenciaCodigo;
  }

  public Integer getEvaluacionId() {
    return evaluacionId;
  }

  public String getNotaAnterior() {
    return notaAnterior;
  }

  public String getNotaNueva() {
    return notaNueva;
  }

  public BigDecimal getPuntajeAnterior() {
    return puntajeAnterior;
  }

  public BigDecimal getPuntajeNuevo() {
    return puntajeNuevo;
  }

  public Boolean getConsolidadaAnterior() {
    return consolidadaAnterior;
  }

  public Boolean getConsolidadaNueva() {
    return consolidadaNueva;
  }

  public Boolean getJustificadaAnterior() {
    return justificadaAnterior;
  }

  public Boolean getJustificadaNueva() {
    return justificadaNueva;
  }

  public String getObservacionAnterior() {
    return observacionAnterior;
  }

  public String getObservacionNueva() {
    return observacionNueva;
  }
}
