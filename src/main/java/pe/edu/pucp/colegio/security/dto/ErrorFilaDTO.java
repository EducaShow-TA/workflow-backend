package pe.edu.pucp.colegio.security.dto;

/** Error de validación de una fila del CSV; {@code fila} cuenta solo filas de datos (desde 1). */
public record ErrorFilaDTO(int fila, String campo, String mensaje) {}
