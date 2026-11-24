package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDateTime;

public record EstadoSistemaResponse(
    LocalDateTime fechaSistema, boolean fechaSimulada, boolean pagoEnLineaDisponible) {}
