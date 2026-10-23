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
    String parentesco) {}
