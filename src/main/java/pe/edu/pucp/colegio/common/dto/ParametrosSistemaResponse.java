package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDateTime;

public record ParametrosSistemaResponse(
    LocalDateTime fechaSistema,
    boolean fechaSimulada,
    boolean fechaSimuladaPermitida,
    String motivoFecha,
    String fechaSimuladaPor,
    LocalDateTime fechaSimuladaEn,
    boolean pagoEnLineaPermitido,
    boolean pagoEnLineaActivo) {}
