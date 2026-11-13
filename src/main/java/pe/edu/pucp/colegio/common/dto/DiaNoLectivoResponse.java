package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;

public record DiaNoLectivoResponse(Integer id, LocalDate fecha, String descripcion) {}
