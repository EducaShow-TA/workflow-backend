package pe.edu.pucp.colegio.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "registros_auditoria")
public class RegistroAuditoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "fecha_hora")
  private LocalDateTime fechaHora = LocalDateTime.now();

  @Column(name = "id_usuario")
  private Integer idUsuario;

  private String rol;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccionAuditoria accion;

  private String descripcion;

  protected RegistroAuditoria() {}

  public RegistroAuditoria(
      Integer idUsuario, String rol, AccionAuditoria accion, String descripcion) {
    this.idUsuario = idUsuario;
    this.rol = rol;
    this.accion = accion;
    this.descripcion = descripcion;
  }
}
