package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Boletas que el apoderado paga en una sola operación desde la pasarela simulada. Los datos de la
 * tarjeta nunca llegan al servidor.
 */
public record PagoApoderadoRequest(@NotEmpty @Size(max = 24) List<@NotNull Integer> boletaIds) {}
