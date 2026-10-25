package pe.edu.pucp.colegio.security.dto;

public record FilaUsuarioCsv(
    int fila,
    String rol,
    String dni,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String correo,
    String telefono,
    String fechaNacimiento,
    String dniApoderado,
    String parentesco,
    String username) {

  public FilaUsuarioCsv(
      int fila,
      String rol,
      String dni,
      String nombres,
      String apellidoPaterno,
      String apellidoMaterno,
      String correo,
      String telefono,
      String fechaNacimiento,
      String dniApoderado,
      String parentesco) {
    this(
        fila,
        rol,
        dni,
        nombres,
        apellidoPaterno,
        apellidoMaterno,
        correo,
        telefono,
        fechaNacimiento,
        dniApoderado,
        parentesco,
        null);
  }
}
