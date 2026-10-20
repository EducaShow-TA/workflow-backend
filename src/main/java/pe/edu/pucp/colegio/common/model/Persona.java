package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "personas")
public class Persona extends EntidadBase {

  @Column(nullable = false, unique = true, length = 15)
  private String dni;

  @Column(nullable = false)
  private String nombre;

  @Column(name = "apellido_paterno", nullable = false)
  private String apellidoPaterno;

  @Column(name = "apellido_materno", nullable = false)
  private String apellidoMaterno;

  private LocalDate fechaNacimiento;
  private String telefono;
  private String genero;

  protected Persona() {}

  public Persona(String dni, String nombre, String apellidoPaterno, String apellidoMaterno) {
    this.dni = dni;
    this.nombre = nombre;
    this.apellidoPaterno = apellidoPaterno;
    this.apellidoMaterno = apellidoMaterno;
    this.genero = "NO_IDENTIFICADO";
  }

  public String getDni() {
    return dni;
  }

  public String getNombre() {
    return nombre;
  }

  public String getApellidoPaterno() {
    return apellidoPaterno;
  }

  public String getApellidoMaterno() {
    return apellidoMaterno;
  }

  public String getNombreCompleto() {
    return nombre + " " + apellidoPaterno + " " + apellidoMaterno;
  }
}
