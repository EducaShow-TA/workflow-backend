package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDateTime;

public record CierreActasResponse(
    Integer periodoId, boolean actasCerradas, LocalDateTime fechaCierre, String usuario) {}
