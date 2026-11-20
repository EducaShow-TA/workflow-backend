package pe.edu.pucp.colegio.common.dto;

/** Parámetros globales vigentes para el marcado de entrada de todos los docentes. */
public record ConfiguracionMarcajeDocenteResponse(
    Integer toleranciaMinutos, Integer ventanaAntesMinutos, Integer ventanaDespuesMinutos) {}
