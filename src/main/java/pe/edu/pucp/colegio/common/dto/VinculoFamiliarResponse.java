package pe.edu.pucp.colegio.common.dto;

public record VinculoFamiliarResponse(
    Integer apoderadoId,
    String tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String celular,
    String correoElectronico,
    String parentesco,
    boolean esPrincipal,
    String username,
    String contrasenhaTemporal) {}
