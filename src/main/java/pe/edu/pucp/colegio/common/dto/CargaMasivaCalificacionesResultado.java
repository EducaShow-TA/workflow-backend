package pe.edu.pucp.colegio.common.dto;

import java.util.List;

public record CargaMasivaCalificacionesResultado(
    int totalFilas, int filasValidas, int filasRechazadas, List<String> errores) {}
