package pe.edu.pucp.colegio.common.dto;

public record GeneracionSesionesResponse(
    Integer periodoId, int creadas, int canceladasPorCalendario, int omitidasNoLectivas) {}
