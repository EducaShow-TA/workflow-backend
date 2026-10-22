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

  @Column(length = 254)
  private String correo;

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

  public void setDni(String dni) {
    this.dni = dni;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getApellidoPaterno() {
    return apellidoPaterno;
  }

  public void setApellidoPaterno(String apellidoPaterno) {
    this.apellidoPaterno = apellidoPaterno;
  }

  public String getApellidoMaterno() {
    return apellidoMaterno;
  }

  public void setApellidoMaterno(String apellidoMaterno) {
    this.apellidoMaterno = apellidoMaterno;
  }

  public LocalDate getFechaNacimiento() {
    return fechaNacimiento;
  }

  public void setFechaNacimiento(LocalDate fechaNacimiento) {
    this.fechaNacimiento = fechaNacimiento;
  }

  public String getTelefono() {
    return telefono;
  }

  public void setTelefono(String telefono) {
    this.telefono = telefono;
  }

  public String getGenero() {
    return genero;
  }

  public String getCorreo() {
    return correo;
  }

  public void setCorreo(String correo) {
    this.correo = correo;
  }

  public void setGenero(String genero) {
    this.genero = genero;
  }

  public String getNombreCompleto() {
    return nombre + " " + apellidoPaterno + " " + apellidoMaterno;
  }
}
