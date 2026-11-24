package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;

public record PagoEnLineaRequest(
    @NotNull(message = "Indica si el pago en línea estará disponible") Boolean activo) {}
